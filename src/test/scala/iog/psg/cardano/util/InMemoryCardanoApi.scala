package iog.psg.cardano.util

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import iog.psg.cardano.{ApiRequestExecutor, CardanoApi}
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse, ErrorMessage}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}

trait InMemoryCardanoApi { this: DummyModel with ScalaFutures with Assertions =>

  implicit val as: ActorSystem
  implicit lazy val ec = as.dispatcher

  final val baseUrl: String = "http://fake:1234/"

  private implicit final class RegexOps(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  implicit final class InMemoryExecutor[T](req: CardanoApiRequest[T]) {
    def executeOrFail(): T = inMemoryExecutor.execute(req).futureValue.getOrElse(fail("Request failed."))
    def executeExpectingErrorOrFail(): ErrorMessage = inMemoryExecutor.execute(req).futureValue.swap.getOrElse(fail("Request should failed."))
  }

  val inMemoryExecutor = new ApiRequestExecutor {
    override def execute[T](
      request: CardanoApi.CardanoApiRequest[T]
    )(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
      val apiAddress = request.request.uri.toString().split(baseUrl).lastOption.getOrElse("")
      val method = request.request.method

      Future.successful {
        ((apiAddress, method) match {
          case ("wallets", HttpMethods.GET) => Right(List(wallet))
          case (s"wallets/${wallet.id}", HttpMethods.GET) => Right(wallet)
          case (r"wallets/.+", HttpMethods.GET) => Left(ErrorMessage(s"Wallet not found", "404"))
          case ("network/information", HttpMethods.GET) => Right(networkInfo)
          case unknown                      => Left(ErrorMessage(s"Not implemented for: $unknown", "400"))
        }).asInstanceOf[CardanoApiResponse[T]]
      }

    }
  }
}
