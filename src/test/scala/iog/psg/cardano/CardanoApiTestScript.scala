package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps._
import iog.psg.cardano.CardanoApi._
import iog.psg.cardano.CardanoApiCodec.TxState.TxState
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, GenericMnemonicSentence, Payment, Payments, QuantityUnit, SyncState, TxState, Units}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object CardanoApiTestScript {


  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = IOExecutionContext(system.dispatcher)

  def main(args: Array[String]): Unit = {

    Try {
      val baseUri = args.headOption.getOrElse(throw new IllegalArgumentException("Pass the base URL to the cardano wallet API as a parameter"))

      val walletNameFrom = "alan1"
      val walletToMnem = GenericMnemonicSentence("enforce chicken cactus pupil wagon brother stuff pumpkin hobby noble genius fish air only sign hour apart fruit market acid beach top subway swear")
      val walletNameTo = "cardano-api-to"
      val walletFromMnem = //GenericMnemonicSentence("sustain noble raise quarter elephant police smile exhibit pass goose acoustic muffin enrich march boy music ostrich maple predict song silk naive trip jump"
        GenericMnemonicSentence("reform illegal victory hurry guard bunker add volume bicycle sock dutch couch target portion soap")
      val walletToPassphrase = "password910"
      val walletFromPassphrase = "1234567890"
      val newWalletPassword = "password!123"

      val lovelaceToTransfer = 10000000

      val walletsNamesOfInterest = Seq(walletNameFrom, walletNameTo)

      println(s"Using base url '$baseUri''")
      println(s"Using wallet name '$walletNameFrom''")

      val api = new CardanoApi(baseUri)

      import api.Ops._

      def waitForTx(txState: TxState, walletId: String, txId: String): Unit = {
        if (txState == TxState.pending) {
          println(s"$txState")
          Thread.sleep(5000)
          val txUpdate = unwrap(api.getTransaction(walletId, txId).toFuture.executeBlocking)
          waitForTx(txUpdate.status, walletId, txId)
        }
        println(s"$txState !!")
      }


      val netInfo = unwrap(api.networkInfo.toFuture.executeBlocking)

      println(netInfo)

      if (netInfo.syncProgress.status == SyncState.ready) {
        val walletAddresses = unwrap(api.listWallets.toFuture.executeBlocking)

        walletAddresses.foreach(addr => {
          println(s"Name: ${addr.name} balance: ${addr.balance}")
          println(s"Id: ${addr.id} pool gap: ${addr.addressPoolGap}")
        })

        val walletsOfInterest = walletAddresses.filter(w => walletsNamesOfInterest.contains(w.name))
        val fromWallet = walletsOfInterest.find(_.name == walletNameFrom).getOrElse {
          println("Generating 'from' wallet...")
          unwrap(api.createRestoreWallet(walletNameFrom, walletFromPassphrase, walletFromMnem).executeBlocking)
        }

        val unitResult = unwrap(
          api.
            updatePassphrase(
              fromWallet.id,
              walletFromPassphrase,
              newWalletPassword)
            .executeBlocking
        )

        /*val fromWalletFromNewPassword = unwrap(
          api.
            createRestoreWallet(
              walletNameFrom,
              newWalletPassword,
              walletFromMnem)
            .executeBlocking
        )*/

        val unitResultPutItBack = unwrap(
          api.
            updatePassphrase(
              fromWallet.id,
              newWalletPassword,
              walletFromPassphrase)
            .executeBlocking
        )

        println(s"From wallet name, id, balance ${fromWallet.name}, ${fromWallet.id}, ${fromWallet.balance}")

        val toWallet = walletsOfInterest.find(_.name == walletNameTo).getOrElse {
          println("Generating 'to' wallet...")
          unwrap(api.createRestoreWallet(walletNameTo, walletToPassphrase, walletToMnem).executeBlocking)
        }

        println(s"To wallet name, id, balance ${toWallet.name}, ${toWallet.id}, ${toWallet.balance}")
        if (fromWallet.balance.available.quantity > 2) {

          val toWalletAddresses =
            unwrap(api.listAddresses(toWallet.id, Some(AddressFilter.unUsed)).toFuture.executeBlocking)


          val paymentTo = toWalletAddresses.headOption.getOrElse(fail("No unused addresses in the To wallet?"))

          val payments = Payments(
            Seq(
              Payment(paymentTo.id, QuantityUnit(lovelaceToTransfer, Units.lovelace))
            )
          )


          val tx = unwrap(api.createTransaction(
            fromWallet.id,
            walletFromPassphrase,
            payments,
            None,
          ).executeBlocking)

          waitForTx(tx.status, fromWallet.id, tx.id)


          val fromWalletAddresses =
            unwrap(api.listAddresses(fromWallet.id, Some(AddressFilter.unUsed)).toFuture.executeBlocking)

          val paymentBack = fromWalletAddresses.headOption.getOrElse(fail("No unused addresses in the From wallet?"))
          //transfer back
          val returnPayments = Payments(
            Seq(
              Payment(paymentBack.id, QuantityUnit(lovelaceToTransfer, Units.lovelace))
            )
          )

          val estimate = unwrap(api.estimateFee(toWallet.id, returnPayments).executeBlocking)
          val returnPayments2 = Payments(
            Seq(
              Payment(paymentBack.id, QuantityUnit(lovelaceToTransfer - estimate.estimatedMax.quantity, Units.lovelace))
            )
          )

          val returnTx = unwrap(api.createTransaction(
            toWallet.id,
            walletToPassphrase,
            returnPayments2,
            None,
          ).executeBlocking)


          waitForTx(returnTx.status, toWallet.id, returnTx.id)

          println(s"Successfully transferred value between 2 wallets")
          val refreshedToWallet = unwrap(api.getWallet(toWallet.id).toFuture.executeBlocking)
          val refreshedFromWallet = unwrap(api.getWallet(fromWallet.id).toFuture.executeBlocking)
          val fromDiffBalance = refreshedFromWallet.balance.available.quantity - fromWallet.balance.available.quantity
          val toDiffBalance = refreshedToWallet.balance.available.quantity - toWallet.balance.available.quantity
          println(s"Balance of 'from' wallet is now ${refreshedFromWallet.balance} diff: $fromDiffBalance")
          println(s"Balance of 'to' wallet is now ${refreshedToWallet.balance} diff $toDiffBalance")

        } else {
          println(s"From wallet ${fromWallet.name} balance ${fromWallet.balance} is too low, cannot continue")

          val fromWalletAddressesFirstFew =
            unwrap(api.listAddresses(fromWallet.id, Some(AddressFilter.unUsed)).toFuture.executeBlocking).take(5)
          println("Use https://testnets.cardano.org/en/cardano/tools/faucet/ to lodge TESTNET credit to below address")
          println(fromWalletAddressesFirstFew)
        }

      } else {
        fail(s"Network not ready ${netInfo.syncProgress}")
      }
    } recover {
      case e => println(e)
    }
    system.terminate()
  }

  private def unwrap[T:ClassTag](apiResult: CardanoApiResponse[T]): T = unwrapOpt(Try(apiResult)).get

  private def unwrapOpt[T: ClassTag](apiResult: Try[CardanoApiResponse[T]]): Option[T] = apiResult match {
    case Success(Left(ErrorMessage(message, code))) =>
      println(s"API Error message $message, code $code")
      None
    case Success(Right(t: T)) => Some(t)
    case Failure(exception) =>
      println(exception)
      None
  }

  private def fail[T](msg: String): T = throw new RuntimeException(msg)


}
