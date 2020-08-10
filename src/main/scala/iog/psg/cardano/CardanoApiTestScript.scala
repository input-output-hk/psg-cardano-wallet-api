package iog.psg.cardano

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import CardanoApiCodec._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object CardanoApiTestScript {

  implicit def toFuture(req: HttpRequest): Future[HttpRequest] = Future.successful(req)

  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val context = system.executionContext
  implicit val waitForDuration = 5.seconds

  def main(args: Array[String]): Unit = {

    val baseUri = args.headOption.getOrElse(throw new IllegalArgumentException("Pass the base URL to the cardano wallet API as a parameter"))
    val walletName = args(1)

    println(s"Using base url '$baseUri''")
    println(s"Using base url '$baseUri''")

    val api = new CardanoApi(baseUri)

    val netInfo = makeBlockingRequest(api.networkInfo, _.toNetworkInfoResponse).getOrElse(fail("Failed to get net work info, url correct?"))

    if (netInfo.syncProgress.status == ready) {
      val walletAddresses = makeBlockingRequest(api.listWallets, _.toWalletAddresses).get
      walletAddresses.foreach(addr => {
        println(s"Name: ${addr.name} balance: ${addr.balance}")
        println(s"Id: ${addr.id} pool gap: ${addr.addressPoolGap}")
      })
      val walletAddress = walletAddresses.headOption.getOrElse {
        val mnem = GenericMnemonicSentence("reform illegal victory hurry guard bunker add volume bicycle sock dutch couch target portion soap")
        //val str = "wrestle trumpet visual ivory security reduce property ecology mutual market mimic cancel liquid mention cluster"
        println("Generating wallet...")
        makeBlockingRequest(
          api.createRestoreWallet(walletName, "password", mnem),
          _.toWalletAddress).get
      }

      val wha = makeBlockingRequest(api.listAddresses(
        ListAddresses(walletAddress.id, None)
      ), _.toFundPaymentsResponse)

      println(wha)

    } else {
      fail(s"Network not ready ${netInfo.syncProgress}")
    }

  }

  def fail[T](msg: String): T = throw new RuntimeException(msg)

  def await[T](a: Awaitable[T]): T = Await.result(a, waitForDuration)


  def makeBlockingRequest[T](reqF: Future[HttpRequest], mapper: HttpResponse => Future[T]): Option[T] = Try {
    await(
      mapper(
        await(
          Http().singleRequest(await(reqF)))
      )
    )
  } match {
    case Failure(e) =>
      println(e)
      None
    case Success(v) =>Some(v)
  }

}
