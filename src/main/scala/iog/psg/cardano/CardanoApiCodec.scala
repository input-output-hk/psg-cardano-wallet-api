package iog.psg.cardano

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives.as
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport
import iog.psg.cardano.CardanoApiCodec.NetworkInfo
import spray.json._
import io.circe.generic.extras._
import io.circe.syntax._
import io.circe.parser.decode
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import io.circe.generic.semiauto.deriveEncoder

import scala.concurrent.{ExecutionContext, Future}

object CardanoApiCodec {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def dropNulls[A](encoder: Encoder[A]): Encoder[A] =
    encoder.mapJson(_.dropNullValues)

  implicit val createRestoreEntityEncoder: Encoder[CreateRestore] = dropNulls(deriveConfiguredEncoder)
  implicit val createListAddrEntityEncoder: Encoder[ListAddresses] = dropNulls(deriveConfiguredEncoder)

  type AddressFilter = String
  val Used: AddressFilter = "used"
  val UnUsed: AddressFilter = "unused"

  type SyncStatus = String
  val ready: SyncStatus = "ready"

  case class SyncProgress(status: String)

  @ConfiguredJsonCodec case class NetworkTip(
                                              epochNumber: Long,
                                              slotNumber: Long,
                                              height: Option[QuantityUnit])

  case class NodeTip(height: QuantityUnit)


  case class ListAddresses(walletId: String, state: Option[AddressFilter])

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

  case class FundPaymentsResponse(inputs: IndexedSeq[InAddress], outputs: Seq[OutAddress])

  @ConfiguredJsonCodec case class WalletAddress(
                                                         id: String,
                                                         addressPoolGap: Int,
                                                         balance: Balance,
                                                         name: String,
                                                         state: State,
                                                         tip: NetworkTip
                                                       )

  implicit class ResponseOps(resp: HttpResponse)(implicit ec: Materializer) {

    def toNetworkInfoResponse: Future[NetworkInfo] = Unmarshal(resp.entity).to[NetworkInfo]

    def toWalletAddress: Future[WalletAddress] = {
      println(resp)
      Unmarshal(resp.entity).to[WalletAddress]
    }

    def toWalletAddresses: Future[Seq[WalletAddress]] = {
      println(resp)
      Unmarshal(resp.entity).to[Seq[WalletAddress]]
    }

    def toFundPaymentsResponse: Future[FundPaymentsResponse] =
      Unmarshal(resp.entity).to[FundPaymentsResponse]
  }


}
