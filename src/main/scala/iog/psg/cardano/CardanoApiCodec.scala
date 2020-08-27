package iog.psg.cardano

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.ContentType.{Binary, WithFixedCharset}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller.eitherUnmarshaller
import akka.stream.Materializer
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.auto._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.CardanoApiCodec.AddressFilter.AddressFilter
import iog.psg.cardano.CardanoApiCodec.SyncState.SyncState
import iog.psg.cardano.CardanoApiCodec.TxDirection.TxDirection
import iog.psg.cardano.CardanoApiCodec.TxState.TxState
import iog.psg.cardano.CardanoApiCodec.Units.Units

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object CardanoApiCodec {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)

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


  @ConfiguredJsonCodec case class NetworkTip(
                                              epochNumber: Long,
                                              slotNumber: Long,
                                              height: Option[QuantityUnit])

  case class NodeTip(height: QuantityUnit)

  case class WalletAddressId(id: String, state: Option[AddressFilter])

  private[cardano] case class CreateTransaction(passphrase: String, payments: Seq[Payment], withdrawal: Option[String])

  private[cardano] case class EstimateFee(payments: Seq[Payment], withdrawal: String)

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
                                                 addressPoolGap: Option[Int] = None
                                               ) {
    require(
      mnemonicSentence.length == 15 ||
        mnemonicSentence.length == 21 ||
        mnemonicSentence.length == 24, s"Mnemonic word list must be 15, 21, or 24 long (not ${mnemonicSentence.length})")
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
                    height: QuantityUnit
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
                                        status: TxState
                                      )

  @ConfiguredJsonCodec
  case class Wallet(
                     id: String,
                     addressPoolGap: Int,
                     balance: Balance,
                     name: String,
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

        case Binary(MediaTypes.`application/octet-stream`) =>
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
