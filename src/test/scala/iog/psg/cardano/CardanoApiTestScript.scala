package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps._
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, maxWaitTime}
import iog.psg.cardano.CardanoApiCodec.{ErrorMessage, GenericMnemonicSentence, Payment, Payments, QuantityUnit, ready}

import scala.util.{Failure, Success, Try}

object CardanoApiTestScript {

  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = system.dispatcher
  //implicit val waitForDuration = 5.seconds

  def main(args: Array[String]): Unit = {

    val baseUri = args.headOption.getOrElse(throw new IllegalArgumentException("Pass the base URL to the cardano wallet API as a parameter"))
    val walletName = args(1)

    println(s"Using base url '$baseUri''")
    println(s"Using wallet name '$walletName''")

    val api = new CardanoApi(baseUri)

    val netInfo = unwrap(api.networkInfo.toFuture.executeBlocking)

    println(netInfo)

    if (netInfo.syncProgress.status == ready) {
      val walletAddresses = unwrap(api.listWallets.toFuture.executeBlocking)

      walletAddresses.foreach(addr => {
        println(s"Name: ${addr.name} balance: ${addr.balance}")
        println(s"Id: ${addr.id} pool gap: ${addr.addressPoolGap}")
      })
      val walletAddress = walletAddresses.headOption.getOrElse {
        val mnem = GenericMnemonicSentence("reform illegal victory hurry guard bunker add volume bicycle sock dutch couch target portion soap")
        val mnem24 = GenericMnemonicSentence("enforce chicken cactus pupil wagon brother stuff pumpkin hobby noble genius fish air only sign hour apart fruit market acid beach top subway swear")
        //val str = "wrestle trumpet visual ivory security reduce property ecology mutual market mimic cancel liquid mention cluster"
        println("Generating wallet...")
        unwrap(api.createRestoreWallet(walletName, "password", mnem24).executeBlocking)
      }

      val allWalletAddresses =
        unwrap(api.listAddresses(walletAddress.id, None).toFuture.executeBlocking)

      println(allWalletAddresses)

      val payments = Payments(
        Seq(
          Payment(allWalletAddresses.head.id, QuantityUnit(1, "lovelace"))
        )
      )

      val paymentsResponse =
        unwrapOpt(api.fundPayments(walletAddress.id, payments).executeBlocking)


      println(paymentsResponse)

    } else {
      fail(s"Network not ready ${netInfo.syncProgress}")
    }

  }

  private def unwrap[T](apiResult: Try[CardanoApiResponse[T]]): T = unwrapOpt(apiResult).get

  private def unwrapOpt[T](apiResult: Try[CardanoApiResponse[T]]): Option[T] = apiResult match {
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
