package iog.psg.cardano

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.ContentType.WithFixedCharset
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller.eitherUnmarshaller
import akka.stream.Materializer
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.CursorOp.DownField
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.syntax.EncoderOps
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.CardanoApiCodec.AddressFilter.AddressFilter
import iog.psg.cardano.CardanoApiCodec.DelegationStatus.DelegationStatus
import iog.psg.cardano.CardanoApiCodec.SyncState.SyncState
import iog.psg.cardano.CardanoApiCodec.TxDirection.TxDirection
import iog.psg.cardano.CardanoApiCodec.TxState.TxState
import iog.psg.cardano.CardanoApiCodec.Units.Units
import org.apache.commons.codec.binary.Hex

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object CardanoApiCodec {

  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  private[cardano] def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)

  private[cardano] implicit val createRestoreEntityEncoder: Encoder[CreateRestore] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val createListAddrEntityEncoder: Encoder[WalletAddressId] = dropNulls(deriveConfiguredEncoder)

  private[cardano] implicit val decodeDateTime: Decoder[ZonedDateTime] = Decoder.decodeString.emap { s =>
    stringToZonedDate(s) match {
      case Success(goodDateTime) => Right(goodDateTime)
      case Failure(exception) => Left(exception.toString)
    }
  }

  private[cardano] implicit val decodeUnits: Decoder[Units] = Decoder.decodeString.map(Units.withName)
  private[cardano] implicit val encodeUnits: Encoder[Units] = (a: Units) => Json.fromString(a.toString)

  private[cardano] implicit val decodeSyncState: Decoder[SyncState] = Decoder.decodeString.map(SyncState.withName)
  private[cardano] implicit val encodeSyncState: Encoder[SyncState] = (a: SyncState) => Json.fromString(a.toString)

  private[cardano] implicit val decodeAddressFilter: Decoder[AddressFilter] = Decoder.decodeString.map(AddressFilter.withName)
  private[cardano] implicit val encodeAddressFilter: Encoder[AddressFilter] = (a: AddressFilter) => Json.fromString(a.toString)

  private[cardano] implicit val decodeTxState: Decoder[TxState] = Decoder.decodeString.map(TxState.withName)
  private[cardano] implicit val encodeTxState: Encoder[TxState] = (a: TxState) => Json.fromString(a.toString)

  private[cardano] implicit val decodeTxDirection: Decoder[TxDirection] = Decoder.decodeString.map(TxDirection.withName)
  private[cardano] implicit val encodeTxDirection: Encoder[TxDirection] = (a: TxDirection) => Json.fromString(a.toString)

  private[cardano] implicit val decodeDelegationStatus: Decoder[DelegationStatus] = Decoder.decodeString.map(DelegationStatus.withName)
  private[cardano] implicit val encodeDelegationStatus: Encoder[DelegationStatus] = (a: DelegationStatus) => Json.fromString(a.toString)

  final case class TxMetadataOut(json: Json) {
    def toMapMetadataStr: Decoder.Result[Map[Long, MetadataValue]] = {
      type KeyVal = Map[Long, MetadataValue]

      // using the expansion may be necessary for Circe to detect it correctly
      implicit val decodeMap: Decoder[Map[Long, MetadataValue]] = new Decoder[Map[Long, MetadataValue]] {
        override def apply(c: HCursor): Decoder.Result[KeyVal] = {

          val valueTypeString = "string"
          val valueTypeLong = "int" //named int but will work as long
          val valueTypeBytes = "bytes"
          val valueTypeList = "list"
          val valueTypeMap = "map"

          def extractStringField(cursor: ACursor): Either[DecodingFailure, MetadataValueStr] =
            cursor.downField(valueTypeString).as[String].fold(
              err => Left(err),
              (value: String) => Right(MetadataValueStr(value))
            )

          def extractLongField(cursor: ACursor): Either[DecodingFailure, MetadataValueLong] =
            cursor.downField(valueTypeLong).as[Long].fold(
              err => Left(err),
              (value: Long) => Right(MetadataValueLong(value))
            )

          def extractBytesField(cursor: ACursor): Either[DecodingFailure, MetadataValueByteString] =
            cursor.downField(valueTypeBytes).as[String].fold(
              err => Left(err),
              (value: String) => Right(MetadataValueByteString(ByteString(value)))
            )

          def extractTypedFieldValue(json: Json): Either[DecodingFailure, MetadataValue] = {
            val cursor = json.hcursor
            cursor.keys.flatMap(_.headOption) match {
              case Some(valueType) if valueType == valueTypeString =>
                extractStringField(cursor)

              case Some(valueType) if valueType == valueTypeLong =>
                extractLongField(cursor)

              case Some(valueType) if valueType == valueTypeBytes =>
                extractBytesField(cursor)
            }
          }

          def extractValueForKeyInto(res: Decoder.Result[KeyVal], key: String): Decoder.Result[KeyVal] = {
            res.flatMap((map: KeyVal) => {
              val keyDownField: ACursor = c.downField(key)
              keyDownField.keys.flatMap(_.headOption) match {
                case Some(valueType) if valueType == valueTypeString =>
                  extractStringField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

                case Some(valueType) if valueType == valueTypeLong =>
                  extractLongField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

                case Some(valueType) if valueType == valueTypeBytes =>
                  extractBytesField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

                case Some(valueType) if valueType == valueTypeList =>
                  val downFieldList = keyDownField.downField(valueTypeList)
                  val keyValuesObjects: List[Json] = downFieldList.values.map(_.toList).getOrElse(Nil)

                  val listResults: Seq[Either[DecodingFailure, MetadataValue]] = keyValuesObjects.map(extractTypedFieldValue)

                  val errors = listResults.filter(_.isLeft)
                  if (errors.nonEmpty) Left(errors.head.swap.toOption.get)
                  else {
                    val values = listResults.flatMap(_.toOption)
                    Right(map.+(key.toLong -> MetadataValueArray(values)))
                  }

                case Some(valueType) if valueType == valueTypeMap =>
                  val downFieldMap = keyDownField.downField(valueTypeMap)
                  val keyValuesObjects: List[Json] = downFieldMap.values.map(_.toList).getOrElse(Nil)

                  def getMapField[T <: MetadataValue](keyName: String, json: Json) = for {
                    keyJson <- json.\\(keyName).headOption.toRight(DecodingFailure(s"Missing '$keyName' value", List(DownField(key))))
                    value <- extractTypedFieldValue(keyJson)
                  } yield value

                  val results: Seq[Either[DecodingFailure, (MetadataKey, MetadataValue)]] = keyValuesObjects.map { json =>
                    (getMapField[MetadataKey]("k", json), getMapField[MetadataValue]("v", json)) match {
                      case (Right(keyField), Right(valueField)) => Right(keyField.asInstanceOf[MetadataKey] -> valueField)
                      case (Left(error), _) => Left(error)
                      case (_ , Left(error)) => Left(error)
                    }
                  }
                  val errors = results.filter(_.isLeft)
                  if (errors.nonEmpty) Left(errors.head.swap.toOption.get)
                  else {
                    val values: Map[MetadataKey, MetadataValue] = results.flatMap(_.toOption).toMap
                    Right(map.+(key.toLong -> MetadataValueMap(values)))
                  }

                case None => Left(DecodingFailure("Missing value under key", List(DownField(key))))
              }
            })
          }

          def emptyMapResult: Decoder.Result[KeyVal] = Right(Map[Long, MetadataValue]().empty)

          def withKeys(keys: Iterable[String]): Decoder.Result[KeyVal] = keys.foldLeft(emptyMapResult)(extractValueForKeyInto)

          c.keys.fold[Decoder.Result[KeyVal]](ifEmpty = emptyMapResult)(withKeys)
        }
      }

      json.as[Map[Long, MetadataValue]](decodeMap)
    }
  }

  private[cardano] implicit val decodeTxMetadataOut: Decoder[TxMetadataOut] = Decoder.decodeJson.map(TxMetadataOut)
  private[cardano] implicit val decodeKeyMetadata: KeyDecoder[MetadataKey] = (key: String) => Some(MetadataValueStr(key))

  sealed trait MetadataValue

  sealed trait MetadataKey extends MetadataValue

  sealed trait TxMetadataIn

  final case class TxMetadataMapIn[K <: Long](m: Map[K, MetadataValue]) extends TxMetadataIn

  object JsonMetadata {
    def apply(str: String): JsonMetadata = JsonMetadata(str.asJson)
  }

  final case class JsonMetadata(metadataCompliantJson: Json) extends TxMetadataIn

  final case class MetadataValueLong(l: Long) extends MetadataKey

  final case class MetadataValueStr(s: String) extends MetadataKey

  final case class MetadataValueArray(ary: Seq[MetadataValue]) extends MetadataValue

  final case class MetadataValueByteString(bs: ByteString) extends MetadataValue

  final case class MetadataValueMap(s: Map[MetadataKey, MetadataValue]) extends MetadataValue

  implicit val metadataKeyDecoder: KeyEncoder[MetadataKey] = {
    case MetadataValueLong(l) => l.toString
    case MetadataValueStr(s) => s
  }

  def toMetadataHex(bs: ByteString): Json = {
    val asHex = Hex.encodeHex(bs.toArray[Byte])
    asHex.asJson
  }

  implicit val encodeTxMeta: Encoder[MetadataValue] = Encoder.instance {
    case MetadataValueLong(s) => Json.obj(("int", Json.fromLong(s)))
    case MetadataValueStr(s) => Json.obj(("string", Json.fromString(s)))
    case MetadataValueByteString(bs: ByteString) => Json.obj(("bytes", Json.fromString(bs.utf8String)))
    case MetadataValueArray(s) => Json.obj(("list", s.asJson))
    case MetadataValueMap(s) =>
      Json.obj(("map", s.map {
        case (key, value) => Map("k" -> key, "v" -> value)
      }.asJson))
  }

  implicit val encodeTxMetadata: Encoder[TxMetadataIn] = Encoder.instance {
    case JsonMetadata(metadataCompliantJson) => metadataCompliantJson
    case TxMetadataMapIn(s) => s.asJson
  }


  object AddressFilter extends Enumeration {
    type AddressFilter = Value

    val used = Value("used")
    val unUsed = Value("unused")

  }

  case class SyncStatus(status: SyncState, progress: Option[QuantityUnit])

  object SyncState extends Enumeration {
    type SyncState = Value
    val ready: SyncState = Value("ready")
    val syncing: SyncState = Value("syncing")
    val notResponding: SyncState = Value("not_responding")

  }

  object DelegationStatus extends Enumeration {
    type DelegationStatus = Value
    val delegating: DelegationStatus = Value("delegating")
    val notDelegating: DelegationStatus = Value("not_delegating")
  }
  final case class DelegationActive(status: DelegationStatus, target: Option[String])
  @ConfiguredJsonCodec final case class DelegationNext(status: DelegationStatus, changesAt: Option[NextEpoch])
  final case class Delegation(active: DelegationActive, next: List[DelegationNext])

  @ConfiguredJsonCodec case class NetworkTip(
                                              epochNumber: Long,
                                              slotNumber: Long,
                                              height: Option[QuantityUnit],
                                              absoluteSlotNumber: Option[Long])

  @ConfiguredJsonCodec case class NodeTip(height: QuantityUnit, slotNumber: Long, epochNumber: Long, absoluteSlotNumber: Option[Long])

  case class WalletAddressId(id: String, state: Option[AddressFilter])

  private[cardano] case class CreateTransaction(
                                                 passphrase: String,
                                                 payments: Seq[Payment],
                                                 metadata: Option[TxMetadataIn],
                                                 withdrawal: Option[String])

  private[cardano] case class EstimateFee(payments: Seq[Payment], withdrawal: Option[String], metadata: Option[TxMetadataIn])

  case class Payments(payments: Seq[Payment])

  case class Payment(address: String, amount: QuantityUnit)

  @ConfiguredJsonCodec private[cardano] case class UpdatePassphrase(oldPassphrase: String, newPassphrase: String)

  trait MnemonicSentence {
    val mnemonicSentence: IndexedSeq[String]
  }

  case class GenericMnemonicSentence(mnemonicSentence: IndexedSeq[String]) extends MnemonicSentence {
    require(
      mnemonicSentence.length == 15 ||
        mnemonicSentence.length == 21 ||
        mnemonicSentence.length == 24, s"Mnemonic word list must be 15, 21, or 24 long (not ${mnemonicSentence.length})")
  }

  object GenericMnemonicSentence {
    def apply(mnemonicString: String): GenericMnemonicSentence =
      GenericMnemonicSentence(mnemonicString.split(" ").toIndexedSeq)
  }

  final case class GenericMnemonicSecondaryFactor(mnemonicSentence: IndexedSeq[String]) extends MnemonicSentence {
    require(
      mnemonicSentence.length == 9 ||
        mnemonicSentence.length == 12, s"Mnemonic word list must be 9, 12 long (not ${mnemonicSentence.length})")
  }

  object GenericMnemonicSecondaryFactor {
    def apply(mnemonicSentence: String): GenericMnemonicSecondaryFactor =
      GenericMnemonicSecondaryFactor(mnemonicSentence.split(" ").toIndexedSeq)
  }

  @ConfiguredJsonCodec
  case class NextEpoch(epochStartTime: ZonedDateTime, epochNumber: Long)

  @ConfiguredJsonCodec
  case class NetworkInfo(
                          syncProgress: SyncStatus,
                          networkTip: NetworkTip,
                          nodeTip: NodeTip,
                          nextEpoch: NextEpoch
                        )

  @ConfiguredJsonCodec
  case class CreateRestore(
                            name: String,
                            passphrase: String,
                            mnemonicSentence: IndexedSeq[String],
                            mnemonicSecondFactor: Option[IndexedSeq[String]] = None,
                            addressPoolGap: Option[Int] = None
                          ) {
    require(
      mnemonicSentence.length == 15 ||
        mnemonicSentence.length == 21 ||
        mnemonicSentence.length == 24, s"Mnemonic word list must be 15, 21, or 24 long (not ${mnemonicSentence.length})")

    private lazy val mnemonicSecondFactorLength = mnemonicSecondFactor.map(_.length).getOrElse(0)
    require(
      mnemonicSecondFactor.isEmpty || (mnemonicSecondFactorLength == 9 || mnemonicSecondFactorLength == 12)
    )
  }

  object Units extends Enumeration {
    type Units = Value
    val percent = Value("percent")
    val lovelace = Value("lovelace")
    val block = Value("block")
  }


  object TxDirection extends Enumeration {
    type TxDirection = Value
    val outgoing = Value("outgoing")
    val incoming = Value("incoming")
  }

  object TxState extends Enumeration {
    type TxState = Value
    val pending = Value("pending")
    val inLedger = Value("in_ledger")
  }

  case class QuantityUnit(
                           quantity: Long,
                           unit: Units
                         )

  case class Balance(
                      available: QuantityUnit,
                      reward: QuantityUnit,
                      total: QuantityUnit
                    )

  case class InAddress(
                        address: Option[String],
                        amount: Option[QuantityUnit],
                        id: String,
                        index: Int)

  case class OutAddress(
                         address: String,
                         amount: QuantityUnit
                       )

  @ConfiguredJsonCodec
  case class StakeAddress(
                           stakeAddress: String,
                           amount: QuantityUnit
                         )

  case class FundPaymentsResponse(
                                   inputs: IndexedSeq[InAddress],
                                   outputs: Seq[OutAddress]
                                 )

  @ConfiguredJsonCodec
  case class Block(
                    slotNumber: Int,
                    epochNumber: Int,
                    height: QuantityUnit,
                    absoluteSlotNumber: Option[Long]
                  )

  @ConfiguredJsonCodec
  case class TimedBlock(
                         time: ZonedDateTime,
                         block: Block
                       )


  @ConfiguredJsonCodec
  case class EstimateFeeResponse(
                                  estimatedMin: QuantityUnit,
                                  estimatedMax: QuantityUnit
                                )

  @ConfiguredJsonCodec
  case class CreateTransactionResponse(
                                        id: String,
                                        amount: QuantityUnit,
                                        insertedAt: Option[TimedBlock],
                                        pendingSince: Option[TimedBlock],
                                        depth: Option[QuantityUnit],
                                        direction: TxDirection,
                                        inputs: Seq[InAddress],
                                        outputs: Seq[OutAddress],
                                        withdrawals: Seq[StakeAddress],
                                        status: TxState,
                                        metadata: Option[TxMetadataOut]
                                      )

  @ConfiguredJsonCodec
  final case class Passphrase(lastUpdatedAt: ZonedDateTime)

  @ConfiguredJsonCodec
  case class Wallet(
                     id: String,
                     addressPoolGap: Int,
                     balance: Balance,
                     delegation: Option[Delegation],
                     name: String,
                     passphrase: Passphrase,
                     state: SyncStatus,
                     tip: NetworkTip
                   )

  def stringToZonedDate(dateAsString: String): Try[ZonedDateTime] = {
    Try(ZonedDateTime.parse(dateAsString, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  }

  def zonedDateToString(zonedDateTime: ZonedDateTime): String = {
    zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
  }

  def toErrorMessage(bs: ByteString): Either[io.circe.Error, ErrorMessage] = {
    import io.circe.parser.decode
    decode[ErrorMessage](bs.utf8String)
  }

  implicit class ResponseOps(response: HttpResponse)(
    implicit mat: Materializer,
    timeout: FiniteDuration,
    ec: ExecutionContext) {

    private def strictEntityF: Future[HttpEntity.Strict] = response.entity.toStrict(timeout)


    private def extractErrorResponse[T](strictEntity: Future[HttpEntity.Strict]): Future[CardanoApiResponse[T]] = {
      strictEntity.map(e => toErrorMessage(e.data) match {
        case Left(err) => Left(ErrorMessage(err.getMessage, "UNPARSEABLE RESULT"))
        case Right(v) => Left(v)
      })

    }

    def toNetworkInfoResponse: Future[CardanoApiResponse[NetworkInfo]]
    = to[NetworkInfo](Unmarshal(_).to[CardanoApiResponse[NetworkInfo]])


    def to[T](f: HttpEntity.Strict => Future[CardanoApiResponse[T]]): Future[CardanoApiResponse[T]] = {

      response.entity.contentType match {
        case WithFixedCharset(MediaTypes.`application/json`) =>
          // Load into memory using toStrict
          // a. no responses utilise streaming and
          // b. the Either unmarshaller requires it
          strictEntityF.flatMap(f)

        case c: ContentType
          if c.mediaType == MediaTypes.`text/plain` ||
            c.mediaType == MediaTypes.`application/octet-stream` =>

          extractErrorResponse[T](strictEntityF)

        case c: ContentType =>
          Future.failed(new RuntimeException(s"Unexpected type ${c.mediaType}, ${c.charsetOption}"))
      }
    }

    def toWallet: Future[CardanoApiResponse[Wallet]]
    = to[Wallet](Unmarshal(_).to[CardanoApiResponse[Wallet]])

    def toWallets: Future[CardanoApiResponse[Seq[Wallet]]]
    = to[Seq[Wallet]](Unmarshal(_).to[CardanoApiResponse[Seq[Wallet]]])


    def toWalletAddressIds: Future[CardanoApiResponse[Seq[WalletAddressId]]]
    = to[Seq[WalletAddressId]](Unmarshal(_).to[CardanoApiResponse[Seq[WalletAddressId]]])

    def toFundPaymentsResponse: Future[CardanoApiResponse[FundPaymentsResponse]]
    = to[FundPaymentsResponse](Unmarshal(_).to[CardanoApiResponse[FundPaymentsResponse]])

    def toCreateTransactionResponses: Future[CardanoApiResponse[Seq[CreateTransactionResponse]]]
    = to[Seq[CreateTransactionResponse]](Unmarshal(_).to[CardanoApiResponse[Seq[CreateTransactionResponse]]])

    def toCreateTransactionResponse: Future[CardanoApiResponse[CreateTransactionResponse]]
    = to[CreateTransactionResponse](Unmarshal(_).to[CardanoApiResponse[CreateTransactionResponse]])

    def toEstimateFeeResponse: Future[CardanoApiResponse[EstimateFeeResponse]] =
      to[EstimateFeeResponse](Unmarshal(_).to[CardanoApiResponse[EstimateFeeResponse]])

    def toUnit: Future[CardanoApiResponse[Unit]] = {
      if (response.status == StatusCodes.NoContent) {
        Future.successful(Right(()))
      } else {
        extractErrorResponse[Unit](strictEntityF)
      }
    }

  }


}
