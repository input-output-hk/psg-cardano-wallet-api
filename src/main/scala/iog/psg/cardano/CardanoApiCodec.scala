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

  private[cardano] implicit val decodeTxMetadataOut: Decoder[TxMetadataOut] = Decoder.decodeJson.map(TxMetadataOut.apply)
  private[cardano] implicit val decodeKeyMetadata: KeyDecoder[MetadataKey] = (key: String) => Some(MetadataValueStr(key))

  private[cardano] implicit val encodeDelegationNext: Encoder[DelegationNext] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val encodeDelegationActive: Encoder[DelegationActive] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val encodeNetworkTip: Encoder[NetworkTip] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val encodeNodeTip: Encoder[NodeTip] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val encodeSyncStatus: Encoder[SyncStatus] = dropNulls(deriveConfiguredEncoder)
  private[cardano] implicit val encodeInAddress: Encoder[InAddress] = dropNulls(deriveConfiguredEncoder)

  sealed trait MetadataValue

  sealed trait MetadataKey extends MetadataValue

  sealed trait TxMetadataIn

  final case class TxMetadataMapIn[K <: Long](m: Map[K, MetadataValue]) extends TxMetadataIn

  object JsonMetadata {
    def apply(rawJson: String): JsonMetadata = parse(rawJson) match {
      case Left(p: ParsingFailure) => throw p
      case Right(jsonMeta) => jsonMeta
    }

    def parse(rawJson: String): Either[ParsingFailure, JsonMetadata] = parser.parse(rawJson).map(JsonMetadata(_))
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

  final case class SyncStatus(status: SyncState, progress: Option[QuantityUnit])

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
  @ConfiguredJsonCodec(decodeOnly = true) final case class DelegationActive(status: DelegationStatus, target: Option[String])
  @ConfiguredJsonCodec(decodeOnly = true) final case class DelegationNext(status: DelegationStatus, changesAt: Option[NextEpoch])
  @ConfiguredJsonCodec final case class Delegation(active: DelegationActive, next: List[DelegationNext])

  @ConfiguredJsonCodec(decodeOnly = true) final case class NetworkTip(
                                              epochNumber: Long,
                                              slotNumber: Long,
                                              height: Option[QuantityUnit],
                                              absoluteSlotNumber: Option[Long])

  @ConfiguredJsonCodec(decodeOnly = true) final case class NodeTip(height: QuantityUnit, slotNumber: Long, epochNumber: Long, absoluteSlotNumber: Option[Long])

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

  @ConfiguredJsonCodec(encodeOnly = true) case class FundPaymentsResponse(
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
