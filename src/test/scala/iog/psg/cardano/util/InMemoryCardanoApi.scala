package iog.psg.cardano.util

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpMethods, HttpResponse, MediaTypes}
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.CardanoApiCodec.AddressFilter
import iog.psg.cardano.{ApiRequestExecutor, CardanoApi}
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

trait InMemoryCardanoApi {
  this: ScalaFutures with Assertions with JsonFiles =>

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

  def httpEntityFromJson(jsonFileName: String,
                         contentType: ContentType = ContentType.WithFixedCharset(MediaTypes.`application/json`)) = {
    val resource = getClass.getResource(s"/jsons/$jsonFileName")
    val file = new File(resource.getFile)
    HttpEntity.fromFile(contentType, file)
  }

  val inMemoryExecutor = new ApiRequestExecutor {
    override def execute[T](
                             request: CardanoApi.CardanoApiRequest[T]
                           )(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
      val apiAddress = request.request.uri.toString().split(baseUrl).lastOption.getOrElse("")
      val method = request.request.method

      (apiAddress, method) match {
        case ("network/information", HttpMethods.GET) =>
          request.mapper(HttpResponse(entity = httpEntityFromJson("netinfo.json")))
      }

      /*Future.successful {
        ((apiAddress, method) match {
            
          case ("wallets", HttpMethods.GET) => Right(List(jsonFileWallet))
          case ("wallets", HttpMethods.POST) => Right(jsonFileWallet)
          case (s"wallets/${jsonFileWallet.id}", HttpMethods.GET) => Right(jsonFileWallet)
          case (s"wallets/${jsonFileWallet.id}", HttpMethods.DELETE) => Right(())
          case (s"wallets/${jsonFileWallet.id}/passphrase", HttpMethods.PUT) => Right(())
          case (s"wallets/${jsonFileWallet.id}/addresses?state=unused", HttpMethods.GET) =>
            Right(jsonFileAddresses.filter(_.state.map(_.toString).contains(AddressFilter.unUsed.toString)))
          case (s"wallets/${jsonFileWallet.id}/addresses?state=used", HttpMethods.GET) =>
            Right(jsonFileAddresses.filter(_.state.map(_.toString).contains(AddressFilter.used.toString)))
          case (s"wallets/${jsonFileWallet.id}/transactions", HttpMethods.GET) =>
            Right(Seq(jsonFileCreatedTransactionResponse))
          case (
            s"wallets/${jsonFileWallet.id}/transactions/${jsonFileCreatedTransactionResponse.id}",
            HttpMethods.GET
            ) =>
            Right(jsonFileCreatedTransactionResponse)
          case (s"wallets/${jsonFileWallet.id}/transactions", HttpMethods.POST) =>
            Right(jsonFileCreatedTransactionResponse)
          case (s"wallets/${jsonFileWallet.id}/payment-fees", HttpMethods.POST) => Right(jsonFileEstimateFees)
          case (s"wallets/${jsonFileWallet.id}/coin-selections/random", HttpMethods.POST) =>
            Right(jsonFileCoinSelectionRandom)
          case (r"wallets/.+/transactions/.+", HttpMethods.GET) => Left(ErrorMessage(s"Transaction not found", "404"))
          case (r"wallets/.+", _) => Left(ErrorMessage(s"Wallet not found", "404"))
          case unknown => Left(ErrorMessage(s"Not implemented for: $unknown", "400"))
        }).asInstanceOf[CardanoApiResponse[T]]
      }*/

    }
  }
}
