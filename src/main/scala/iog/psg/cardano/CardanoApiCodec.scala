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
import io.circe.generic.extras.{ConfiguredJsonCodec, _}
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

  object ImplicitCodecs {
    implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

    implicit val createRestoreEntityEncoder: Encoder[CreateRestore] = dropNulls(deriveConfiguredEncoder)
    implicit val createListAddrEntityEncoder: Encoder[WalletAddressId] = dropNulls(deriveConfiguredEncoder)

    implicit val decodeDateTime: Decoder[ZonedDateTime] = Decoder.decodeString.emap { s =>
      stringToZonedDate(s) match {
        case Success(goodDateTime) => Right(goodDateTime)
        case Failure(exception) => Left(exception.toString)
      }
    }

    implicit val decodeUnits: Decoder[Units] = Decoder.decodeString.map(Units.withName)
    implicit val encodeUnits: Encoder[Units] = (a: Units) => Json.fromString(a.toString)

    implicit val decodeSyncState: Decoder[SyncState] = Decoder.decodeString.map(SyncState.withName)
    implicit val encodeSyncState: Encoder[SyncState] = (a: SyncState) => Json.fromString(a.toString)

    implicit val decodeAddressFilter: Decoder[AddressFilter] = Decoder.decodeString.map(AddressFilter.withName)
    implicit val encodeAddressFilter: Encoder[AddressFilter] = (a: AddressFilter) => Json.fromString(a.toString)

    implicit val decodeTxState: Decoder[TxState] = Decoder.decodeString.map(TxState.withName)
    implicit val encodeTxState: Encoder[TxState] = (a: TxState) => Json.fromString(a.toString)

    implicit val decodeTxDirection: Decoder[TxDirection] = Decoder.decodeString.map(TxDirection.withName)
    implicit val encodeTxDirection: Encoder[TxDirection] = (a: TxDirection) => Json.fromString(a.toString)

    implicit val decodeDelegationStatus: Decoder[DelegationStatus] = Decoder.decodeString.map(DelegationStatus.withName)
    implicit val encodeDelegationStatus: Encoder[DelegationStatus] = (a: DelegationStatus) => Json.fromString(a.toString)

    implicit val decodeTxMetadataOut: Decoder[TxMetadataOut] = Decoder.decodeJson.map(TxMetadataOut.apply)
    implicit val decodeKeyMetadata: KeyDecoder[MetadataKey] = (key: String) => Some(MetadataValueStr(key))

    implicit val encodeDelegationNext: Encoder[DelegationNext] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeDelegationActive: Encoder[DelegationActive] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeNetworkTip: Encoder[NetworkTip] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeNodeTip: Encoder[NodeTip] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeSyncStatus: Encoder[SyncStatus] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeCreateTransactionResponse: Encoder[CreateTransactionResponse] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeWallet: Encoder[Wallet] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeBlock: Encoder[Block] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeWalletAddress: Encoder[WalletAddress] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeSubmitMigrationResponse: Encoder[MigrationResponse] = dropNulls(deriveConfiguredEncoder)
    implicit val encodeStakePool: Encoder[StakePool] = dropNulls(deriveConfiguredEncoder)

    private def decodeQuantityUnit[T](c: HCursor)(implicit d: Decoder[T]) = for {
      quantity <- c.downField("quantity").as[T]
      unitsStr <- c.downField("unit").as[String]
      units <- Try(Units.withName(unitsStr)).toEither.left.map(_ => DecodingFailure("unit", c.history))
    } yield {
      QuantityUnit(quantity, units)
    }

    implicit val decodeQuantityUnitL: Decoder[QuantityUnit[Long]] =
      (c: HCursor) => decodeQuantityUnit[Long](c)

    implicit val decodeQuantityUnitD: Decoder[QuantityUnit[Double]] =
      (c: HCursor) => decodeQuantityUnit[Double](c)


    implicit val metadataKeyDecoder: KeyEncoder[MetadataKey] = {
      case MetadataValueLong(l) => l.toString
      case MetadataValueStr(s) => s
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
  }

  import ImplicitCodecs._

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)

  def toMetadataHex(bs: ByteString): Json = {
    val asHex = Hex.encodeHex(bs.toArray[Byte])
    asHex.asJson
  }

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


  object AddressFilter extends Enumeration {
    type AddressFilter = Value

    val used = Value("used")
    val unUsed = Value("unused")

  }

  final case class SyncStatus(status: SyncState, progress: Option[QuantityUnit[Double]])

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
                                              height: Option[QuantityUnit[Long]],
                                              absoluteSlotNumber: Option[Long])

  @ConfiguredJsonCodec(decodeOnly = true) final case class NodeTip(height: QuantityUnit[Long], slotNumber: Long, epochNumber: Long, absoluteSlotNumber: Option[Long])

  case class WalletAddressId(id: String, state: Option[AddressFilter])

  @ConfiguredJsonCodec(decodeOnly = true) final case class Pointer(slotNum: Long, transactionIndex: Long, outputIndex: Long)

  @ConfiguredJsonCodec(decodeOnly = true) final case class WalletAddress(
                                                                          addressStyle: String,
                                                                          stakeReference: String,
                                                                          networkTag: Long,
                                                                          spendingKeyHash: String,
                                                                          stakeKeyHash: String,
                                                                          scriptHash: Option[String],
                                                                          pointer: Option[Pointer],
                                                                          addressRoot: Option[String],
                                                                          derivationPath: Option[String])

  private[cardano] case class CreateTransaction(
                                                 passphrase: String,
                                                 payments: Seq[Payment],
                                                 metadata: Option[TxMetadataIn],
                                                 withdrawal: Option[String])

  private[cardano] case class EstimateFee(payments: Seq[Payment], withdrawal: Option[String], metadata: Option[TxMetadataIn])

  case class Payments(payments: Seq[Payment])

  case class Payment(address: String, amount: QuantityUnit[Long])

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
  final case class NetworkClock(
                                 status: String,
                                 offset: QuantityUnit[Long]
                               )

  @ConfiguredJsonCodec
  final case class NetworkParameters(
                                      genesisBlockHash: String,
                                      blockchain_start_time: ZonedDateTime,
                                      slotLength: QuantityUnit[Long],
                                      epochLength: QuantityUnit[Long],
                                      epochStability: QuantityUnit[Long],
                                      activeSlotCoefficient: QuantityUnit[Long],
                                      decentralizationLevel: QuantityUnit[Long],
                                      desiredPoolNumber: Long,
                                      minimumUtxoValue: QuantityUnit[Long],
                                      hardforkAt: NextEpoch
                                    )

  @ConfiguredJsonCodec(decodeOnly = true)
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
    val slot = Value("slot")
    val microsecond = Value("microsecond")
    val second = Value("second")
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

  @ConfiguredJsonCodec(encodeOnly = true) final case class QuantityUnit[T] private(quantity: T, unit: Units)

  object QuantityUnit {
    def apply(quantity: Long, unit: Units): QuantityUnit[Long] = new QuantityUnit(quantity, unit)
    def apply(quantity: Double, unit: Units): QuantityUnit[Double] = new QuantityUnit(quantity, unit)
  }

  case class Balance(
                      available: QuantityUnit[Long],
                      reward: QuantityUnit[Long],
                      total: QuantityUnit[Long]
                    )

  case class InAddress(
                        address: Option[String],
                        amount: Option[QuantityUnit[Long]],
                        id: String,
                        index: Int)

  case class OutAddress(
                         address: String,
                         amount: QuantityUnit[Long]
                       )

  @ConfiguredJsonCodec
  case class StakeAddress(
                           stakeAddress: String,
                           amount: QuantityUnit[Long]
                         )

  case class FundPaymentsResponse(
                                   inputs: IndexedSeq[InAddress],
                                   outputs: Seq[OutAddress]
                                 )

  @ConfiguredJsonCodec(decodeOnly = true)
  case class Block(
                    slotNumber: Int,
                    epochNumber: Int,
                    height: QuantityUnit[Long],
                    absoluteSlotNumber: Option[Long]
                  )

  @ConfiguredJsonCodec(decodeOnly = true)
  case class TimedBlock(
                         time: ZonedDateTime,
                         block: Block
                       )

  @ConfiguredJsonCodec
  final case class TimedFlattenBlock(
                                      time: ZonedDateTime,
                                      slotNumber: Int,
                                      epochNumber: Int,
                                      height: Option[QuantityUnit[Long]],
                                      absoluteSlotNumber: Option[Long]
                                    )

  @ConfiguredJsonCodec
  case class EstimateFeeResponse(
                                  estimatedMin: QuantityUnit[Long],
                                  estimatedMax: QuantityUnit[Long]
                                )

  @ConfiguredJsonCodec(decodeOnly = true)
  case class CreateTransactionResponse(
                                        id: String,
                                        amount: QuantityUnit[Long],
                                        insertedAt: Option[TimedBlock],
                                        pendingSince: Option[TimedBlock],
                                        depth: Option[QuantityUnit[Long]],
                                        direction: TxDirection,
                                        inputs: Seq[InAddress],
                                        outputs: Seq[OutAddress],
                                        withdrawals: Seq[StakeAddress],
                                        status: TxState,
                                        metadata: Option[TxMetadataOut]
                                      )

  @ConfiguredJsonCodec(decodeOnly = true)
  final case class MigrationResponse(
                                        id: String,
                                        amount: QuantityUnit[Long],
                                        insertedAt: Option[TimedBlock],
                                        pendingSince: Option[TimedBlock],
                                        expiresAt: Option[TimedBlock],
                                        depth: Option[QuantityUnit[Long]],
                                        direction: TxDirection,
                                        inputs: Seq[InAddress],
                                        outputs: Seq[OutAddress],
                                        withdrawals: Seq[StakeAddress],
                                        status: TxState,
                                        metadata: Option[TxMetadataOut]
                                      )

  @ConfiguredJsonCodec
  final case class MigrationCostResponse(migrationCost: QuantityUnit[Long], leftovers: QuantityUnit[Long])

  @ConfiguredJsonCodec
  final case class Passphrase(lastUpdatedAt: ZonedDateTime)

  @ConfiguredJsonCodec(decodeOnly = true)
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

  @ConfiguredJsonCodec
  final case class UTxOStatistics(total: QuantityUnit[Long], scale: String, distribution: Map[String, Long])

  @ConfiguredJsonCodec
  final case class PostExternalTransactionResponse(id: String)

  @ConfiguredJsonCodec(encodeOnly = true)
  final case class SubmitMigration(passphrase: String, addresses: Seq[String])

  @ConfiguredJsonCodec
  final case class StakePoolMetric(
                                    nonMyopicMemberRewards: QuantityUnit[Long],
                                    relativeStake: QuantityUnit[Double],
                                    saturation: Double,
                                    producedBlocks: QuantityUnit[Long]
                                  )

  @ConfiguredJsonCodec
  final case class StakePoolMetadata(
                                      ticker: String,
                                      name: String,
                                      description: String,
                                      homepage: String
                                    )

  @ConfiguredJsonCodec(decodeOnly = true)
  final case class StakePool(
                              id: String,
                              metrics: StakePoolMetric,
                              cost: QuantityUnit[Long],
                              margin: QuantityUnit[Double],
                              pledge: QuantityUnit[Long],
                              metadata: Option[StakePoolMetadata],
                              retirement: Option[NextEpoch]
                            )

  @ConfiguredJsonCodec(encodeOnly = true)
  final case class PassphraseRequest(passphrase: String)

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

    def toNetworkClockResponse: Future[CardanoApiResponse[NetworkClock]]
    = to[NetworkClock](Unmarshal(_).to[CardanoApiResponse[NetworkClock]])

    def toNetworkParametersResponse: Future[CardanoApiResponse[NetworkParameters]]
    = to[NetworkParameters](Unmarshal(_).to[CardanoApiResponse[NetworkParameters]])

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

    def toWalletAddress: Future[CardanoApiResponse[WalletAddress]]
    = to[WalletAddress](Unmarshal(_).to[CardanoApiResponse[WalletAddress]])

    def toFundPaymentsResponse: Future[CardanoApiResponse[FundPaymentsResponse]]
    = to[FundPaymentsResponse](Unmarshal(_).to[CardanoApiResponse[FundPaymentsResponse]])

    def toCreateTransactionResponses: Future[CardanoApiResponse[Seq[CreateTransactionResponse]]]
    = to[Seq[CreateTransactionResponse]](Unmarshal(_).to[CardanoApiResponse[Seq[CreateTransactionResponse]]])

    def toCreateTransactionResponse: Future[CardanoApiResponse[CreateTransactionResponse]]
    = to[CreateTransactionResponse](Unmarshal(_).to[CardanoApiResponse[CreateTransactionResponse]])

    def toEstimateFeeResponse: Future[CardanoApiResponse[EstimateFeeResponse]] =
      to[EstimateFeeResponse](Unmarshal(_).to[CardanoApiResponse[EstimateFeeResponse]])

    def toUTxOStatisticsResponse: Future[CardanoApiResponse[UTxOStatistics]] =
      to[UTxOStatistics](Unmarshal(_).to[CardanoApiResponse[UTxOStatistics]])

    def toPostExternalTransactionResponse: Future[CardanoApiResponse[PostExternalTransactionResponse]] =
      to[PostExternalTransactionResponse](Unmarshal(_).to[CardanoApiResponse[PostExternalTransactionResponse]])

    def toMigrationResponse: Future[CardanoApiResponse[MigrationResponse]] =
      to[MigrationResponse](Unmarshal(_).to[CardanoApiResponse[MigrationResponse]])

    def toMigrationsResponse: Future[CardanoApiResponse[Seq[MigrationResponse]]] =
      to[Seq[MigrationResponse]](Unmarshal(_).to[CardanoApiResponse[Seq[MigrationResponse]]])

    def toMigrationCostResponse: Future[CardanoApiResponse[MigrationCostResponse]] =
      to[MigrationCostResponse](Unmarshal(_).to[CardanoApiResponse[MigrationCostResponse]])

    def toStakePoolsResponse: Future[CardanoApiResponse[Seq[StakePool]]] =
      to[Seq[StakePool]](Unmarshal(_).to[CardanoApiResponse[Seq[StakePool]]])

    def toUnit: Future[CardanoApiResponse[Unit]] = {
      if (response.status == StatusCodes.NoContent) {
        Future.successful(Right(()))
      } else {
        extractErrorResponse[Unit](strictEntityF)
      }
    }

  }


}
