package iog.psg.cardano.util

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.CardanoApiCodec.AddressFilter
import iog.psg.cardano.{ApiRequestExecutor, CardanoApi}
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
    def executeExpectingErrorOrFail(): ErrorMessage =
      inMemoryExecutor.execute(req).futureValue.swap.getOrElse(fail("Request should failed."))
  }

  implicit final class InMemoryFExecutor[T](req: Future[CardanoApiRequest[T]]) {
    def executeOrFail(): T = inMemoryExecutor.execute(req.futureValue).futureValue.getOrElse(fail("Request failed."))
    def executeExpectingErrorOrFail(): ErrorMessage =
      inMemoryExecutor.execute(req.futureValue).futureValue.swap.getOrElse(fail("Request should failed."))
  }

  val inMemoryExecutor = new ApiRequestExecutor {
    override def execute[T](
      request: CardanoApi.CardanoApiRequest[T]
    )(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
      val apiAddress = request.request.uri.toString().split(baseUrl).lastOption.getOrElse("")
      val method = request.request.method

      Future.successful {
        ((apiAddress, method) match {
          case ("network/information", HttpMethods.GET) => Right(networkInfo)
          case ("wallets", HttpMethods.GET)                          => Right(List(wallet))
          case ("wallets", HttpMethods.POST)                         => Right(wallet)
          case (s"wallets/${wallet.id}", HttpMethods.GET)            => Right(wallet)
          case (s"wallets/${wallet.id}", HttpMethods.DELETE)         => Right(())
          case (s"wallets/${wallet.id}/passphrase", HttpMethods.PUT) => Right(())
          case (s"wallets/${wallet.id}/addresses?state=unused", HttpMethods.GET) =>
            Right(addresses.filter(_.state.contains(AddressFilter.unUsed)))
          case (s"wallets/${wallet.id}/addresses?state=used", HttpMethods.GET) =>
            Right(addresses.filter(_.state.contains(AddressFilter.used)))
          case (s"wallets/${wallet.id}/transactions", HttpMethods.GET) => Right(Seq(createdTransactionResponse))
          case (s"wallets/${wallet.id}/transactions/${createdTransactionResponse.id}", HttpMethods.GET) =>
            Right(createdTransactionResponse)
          case (s"wallets/${wallet.id}/transactions", HttpMethods.POST)           => Right(createdTransactionResponse)
          case (s"wallets/${wallet.id}/payment-fees", HttpMethods.POST)           => Right(estimateFeeResponse)
          case (s"wallets/${wallet.id}/coin-selections/random", HttpMethods.POST) => Right(fundPaymentsResponse)
          case (r"wallets/.+/transactions/.+", HttpMethods.GET) => Left(ErrorMessage(s"Transaction not found", "404"))
          case (r"wallets/.+", _) => Left(ErrorMessage(s"Wallet not found", "404"))
          case unknown            => Left(ErrorMessage(s"Not implemented for: $unknown", "400"))
        }).asInstanceOf[CardanoApiResponse[T]]
      }

    }
  }
}
