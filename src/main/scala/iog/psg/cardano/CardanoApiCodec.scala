package iog.psg.cardano

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling.Unmarshaller.eitherUnmarshaller
import akka.stream.Materializer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import iog.psg.cardano.CardanoApi.CardanoApiResponse

import scala.concurrent.Future

object CardanoApiCodec {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)

  implicit val createRestoreEntityEncoder: Encoder[CreateRestore] = dropNulls(deriveConfiguredEncoder)
  implicit val createListAddrEntityEncoder: Encoder[WalletAddressId] = dropNulls(deriveConfiguredEncoder)

  type AddressFilter = String
  val Used: AddressFilter = "used"
  val UnUsed: AddressFilter = "unused"

  type SyncStatus = String
  val ready: SyncStatus = "ready"

  case class ErrorMessage(message: String, code: String)

  case class SyncProgress(status: String)

  @ConfiguredJsonCodec case class NetworkTip(
                                              epochNumber: Long,
                                              slotNumber: Long,
                                              height: Option[QuantityUnit])

  case class NodeTip(height: QuantityUnit)

  case class WalletAddressId(id: String, state: Option[AddressFilter])

  case class CreateTransaction(passphrase: String, payments: Seq[Payment], withdrawal: Option[String])

  case class Payments(payments: Seq[Payment])

  case class Payment(address: String, amount: QuantityUnit)

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

  @ConfiguredJsonCodec case class NextEpoch(epochStartTime: String, epochNumber: Long)

  @ConfiguredJsonCodec case class NetworkInfo(
                                               syncProgress: SyncProgress,
                                               networkTip: NetworkTip,
                                               nodeTip: NodeTip,
                                               nextEpoch: NextEpoch
                                             )

  @ConfiguredJsonCodec case class CreateRestore(
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

  case class QuantityUnit(quantity: Long, unit: String)

  case class Balance(available: QuantityUnit, reward: QuantityUnit, total: QuantityUnit)

  case class State(status: String)

  trait Address {
    val address: String
    val amount: QuantityUnit
  }

  case class InAddress(address: String, amount: QuantityUnit, id: String, index: Int) extends Address

  case class OutAddress(address: String, amount: QuantityUnit) extends Address

  @ConfiguredJsonCodec case class StakeAddress(stakeAddress: String, amount: QuantityUnit)

  case class Transaction(inputs: IndexedSeq[InAddress], outputs: Seq[OutAddress])

  case class FundPaymentsResponse(inputs: IndexedSeq[InAddress], outputs: Seq[OutAddress])

  @ConfiguredJsonCodec case class Block(slotNumber: Int, epochNumber: Int, height: QuantityUnit)
  @ConfiguredJsonCodec case class TimedBlock(time: String, block: Block)

  @ConfiguredJsonCodec case class CreateTransactionResponse(
                                                             id: String,
                                                             amount: QuantityUnit,
                                                             insertedAt: TimedBlock,
                                                             pendingSince: TimedBlock,
                                                             depth: QuantityUnit,
                                                             direction: String,
                                                             inputs: Seq[InAddress],
                                                             outputs: Seq[OutAddress],
                                                             withdrawals: Seq[StakeAddress],
                                                             status: String
                                                           )

  @ConfiguredJsonCodec case class Wallet(
                                          id: String,
                                          addressPoolGap: Int,
                                          balance: Balance,
                                          name: String,
                                          state: State,
                                          tip: NetworkTip
                                        )

  implicit class ResponseOps(strictEntity: HttpEntity.Strict)(implicit ec: Materializer) {

    def toNetworkInfoResponse: Future[CardanoApiResponse[NetworkInfo]] = {
      Unmarshal(strictEntity).to[CardanoApiResponse[NetworkInfo]]
    }

    def toWallet: Future[CardanoApiResponse[Wallet]] = {
      println(strictEntity)
      Unmarshal(strictEntity).to[CardanoApiResponse[Wallet]]
    }

    def toWallets: Future[CardanoApiResponse[Seq[Wallet]]] = {
      println(strictEntity)
      Unmarshal(strictEntity).to[CardanoApiResponse[Seq[Wallet]]]
    }


    def toWalletAddressIds: Future[CardanoApiResponse[Seq[WalletAddressId]]] = {
      println(strictEntity)
      Unmarshal(strictEntity).to[CardanoApiResponse[Seq[WalletAddressId]]]
    }

    def toFundPaymentsResponse: Future[CardanoApiResponse[FundPaymentsResponse]] = {
      (Unmarshal(strictEntity).to[Either[ErrorMessage, FundPaymentsResponse]])
    }

    def toWalletTransactions: Future[CardanoApiResponse[Seq[Transaction]]] = {
      (Unmarshal(strictEntity).to[Either[ErrorMessage, Seq[Transaction]]])
    }

    def toCreateTransactionResponse: Future[CardanoApiResponse[CreateTransactionResponse]] = {
      (Unmarshal(strictEntity).to[Either[ErrorMessage, CreateTransactionResponse]])
    }

  }


}
