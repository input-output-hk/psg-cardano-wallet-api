package iog.psg.cardano

import java.util

import akka.actor.ActorSystem
import akka.util.ByteString
import iog.psg.cardano.CardanoApi.CardanoApiOps._
import iog.psg.cardano.CardanoApi._
import iog.psg.cardano.CardanoApiCodec.TxState.TxState
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, CreateTransactionResponse, GenericMnemonicSentence, MetadataValueArray, MetadataValueByteArray, MetadataValueLong, MetadataValueStr, Payment, Payments, QuantityUnit, SyncState, TxMetadataMapIn, TxState, Units}
import org.apache.commons.codec.binary.Hex

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.{Failure, Random, Success, Try}

/**
 * This script ran successfully in Aug 2020
 * It's purpose was to explore how the API works.
 * It is left as a hint/starting point for developers as to how to use the API basics.
 */
object CardanoApiTestScript {


  private implicit val system = ActorSystem("SingleRequest")


  def main(args: Array[String]): Unit = {

    Try {
      val baseUri = args.headOption.getOrElse(throw new IllegalArgumentException("Pass the base URL to the cardano wallet API as a parameter"))

      val walletNameFrom = "alan1"
      val walletToMnem = GenericMnemonicSentence("enforce chicken cactus pupil wagon brother stuff pumpkin hobby noble genius fish air only sign hour apart fruit market acid beach top subway swear")
      val walletNameTo = "cardano-api-to"
      val walletFromMnem =
        GenericMnemonicSentence("reform illegal victory hurry guard bunker add volume bicycle sock dutch couch target portion soap")
      val walletToPassphrase = "password910"
      val walletFromPassphrase = "1234567890"
      val newWalletPassword = "password!123"

      val lovelaceToTransfer = 10000000

      val walletsNamesOfInterest = Seq(walletNameFrom, walletNameTo)

      println(s"Using base url '$baseUri''")
      println(s"Using wallet name '$walletNameFrom''")

      import system.dispatcher
      val api = new CardanoApi(baseUri)


      @tailrec
      def waitForTx(txCreateResponse: CreateTransactionResponse, walletId: String, txId: String): CreateTransactionResponse = {
        if (txCreateResponse.status == TxState.pending) {
          println(s"Wait for ${TxState.inLedger} ${txCreateResponse.status}")
          Thread.sleep(5000)
          val txUpdate = unwrap(api.getTransaction(walletId, txId).toFuture.executeBlocking)
          waitForTx(txUpdate, walletId, txId)
        } else {
          txCreateResponse
        }
      }


      val netInfo = unwrap(api.networkInfo.toFuture.executeBlocking)

      println(netInfo)

      if (netInfo.syncProgress.status == SyncState.ready) {
        val walletAddresses = unwrap(api.listWallets.toFuture.executeBlocking)


        val walletsOfInterest = walletAddresses.filter(w => walletsNamesOfInterest.contains(w.name))
        val fromWallet = walletsOfInterest.find(_.name == walletNameFrom).getOrElse {
          println("Generating 'from' wallet...")
          unwrap(api.createRestoreWallet(walletNameFrom, walletFromPassphrase, walletFromMnem).executeBlocking)
        }

        println(s"From wallet name, id, balance ${fromWallet.name}, ${fromWallet.id}, ${fromWallet.balance}")

        if (fromWallet.balance.available.quantity > 2) {

          val toWalletAddresses =
            unwrap(api.listAddresses(fromWallet.id, Some(AddressFilter.unUsed)).toFuture.executeBlocking)

          val paymentTo = toWalletAddresses.headOption.getOrElse(fail("No unused addresses in the To wallet?"))

          val payments = Payments(
            Seq(
              Payment(paymentTo.id, QuantityUnit(lovelaceToTransfer, Units.lovelace))
            )
          )

          val hexAry = Hex.encodeHex(("" + ("1" * 12)).getBytes())
          val h = new String(hexAry)
          val inAry = Array.fill(1024 * 5)(Random.nextBytes(1).head)
          val ha = MetadataValueArray(inAry.toSeq.map(MetadataValueLong(_)))
          MetadataValueStr(h)
          val meta = TxMetadataMapIn(
            Map(6L ->
              ha
            )
          )

          val tx = unwrap(api.createTransaction(
            fromWallet.id,
            walletFromPassphrase,
            payments,
            Some(meta),
            None,
          ).executeBlocking)

          val finalTx = waitForTx(tx, fromWallet.id, tx.id)

          println(s"Last Tx in stream ${finalTx.metadata}")
          val back = finalTx.metadata.get.json.as[Map[Long, Array[Byte]]]
          back match {
            case Left(err) =>
            case Right(s) =>
              val good = util.Arrays.equals(s(6),inAry)
              println(good)
          }

          println(s"Successfully transferred value from wallet to wallet")

          val refreshedFromWallet = unwrap(api.getWallet(fromWallet.id).toFuture.executeBlocking)
          val fromDiffBalance = refreshedFromWallet.balance.available.quantity - fromWallet.balance.available.quantity
          println(s"Balance of 'from' wallet is now ${refreshedFromWallet.balance} diff: $fromDiffBalance")

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

  private def unwrap[T: ClassTag](apiResult: CardanoApiResponse[T]): T = unwrapOpt(Try(apiResult)).get

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
