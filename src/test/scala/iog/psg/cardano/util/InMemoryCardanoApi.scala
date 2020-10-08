package iog.psg.cardano.util

import java.io.File
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.generic.auto._
import io.circe.{ parser, Json, ParsingFailure }
import io.circe.syntax._
import iog.psg.cardano.CardanoApi.Order.Order
import iog.psg.cardano.CardanoApi.{ CardanoApiRequest, CardanoApiResponse, ErrorMessage, Order }
import iog.psg.cardano.CardanoApiCodec.AddressFilter
import iog.psg.cardano.jpi.CardanoApiException
import iog.psg.cardano.{ ApiRequestExecutor, CardanoApi }
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ ExecutionContext, Future }

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

  private def httpEntityFromJson(
                                  jsonFileName: String,
                                  contentType: ContentType = ContentType.WithFixedCharset(MediaTypes.`application/json`)
                                ) = {
    val resource = getClass.getResource(s"/jsons/$jsonFileName")
    val file = new File(resource.getFile)
    HttpEntity.fromFile(contentType, file)
  }

  private def getTransactions(
                               walletId: String,
                               start: ZonedDateTime,
                               end: ZonedDateTime,
                               order: Order,
                               minWithdrawal: Int
                             ) =
    jsonFileCreatedTransactionsResponse.filter { transaction =>
      val matchesDates = transaction.insertedAt.isEmpty || transaction.insertedAt.exists { tb =>
        val afterStart = start.isBefore(tb.time)
        val beforeEnd = end.isAfter(tb.time)

        afterStart && beforeEnd
      }

      matchesDates && transaction.withdrawals.exists(wd => wd.amount.quantity >= minWithdrawal)
    }.sortWith((ta, tb) => if (order == Order.descendingOrder) ta.id > tb.id else ta.id < tb.id)

  val inMemoryExecutor: ApiRequestExecutor = new ApiRequestExecutor {
    override def execute[T](
                             request: CardanoApi.CardanoApiRequest[T]
                           )(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
      val apiAddress = request.request.uri.toString().split(baseUrl).lastOption.getOrElse("")
      val method = request.request.method
      lazy val query = request.request.uri.query().toMap

      implicit def univEntToHttpResponse[T](ue: UniversalEntity): HttpResponse =
        HttpResponse(entity = ue)

      def notFound(msg: String) = {
        val json: String = ErrorMessage(msg, "404").asJson.noSpaces
        val entity = HttpEntity(json)
        request.mapper(HttpResponse(status = StatusCodes.NotFound, entity = entity))
      }

      def toJsonResponse[A](resp: A)(implicit enc: io.circe.Encoder[A]) =
        request.mapper(
          HttpEntity(resp.asJson.noSpaces)
            .withContentType(ContentType.WithFixedCharset(MediaTypes.`application/json`))
        )

      def parseZonedDT(name: String) = ZonedDateTime.parse(query(name))

      (apiAddress, method) match {
        case ("network/information", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("netinfo.json"))

        case ("wallets", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("wallets.json"))

        case ("wallets", HttpMethods.POST) =>
          request.mapper(httpEntityFromJson("wallet.json"))

        case (s"wallets/${jsonFileWallet.id}", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("wallet.json"))

        case (s"wallets/${jsonFileWallet.id}", HttpMethods.DELETE) =>
          request.mapper(HttpResponse(status = StatusCodes.NoContent))

        case (s"wallets/${jsonFileWallet.id}/passphrase", HttpMethods.PUT) =>
          request.mapper(HttpResponse(status = StatusCodes.NoContent))

        case (s"wallets/${jsonFileWallet.id}/addresses?state=unused", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("unused_addresses.json"))

        case (s"wallets/${jsonFileWallet.id}/addresses?state=used", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("used_addresses.json"))

        case (s"wallets/${jsonFileWallet.id}/transactions?order=descending", HttpMethods.GET) =>
          toJsonResponse(jsonFileCreatedTransactionsResponse.sortWith(_.id > _.id))

        case (s"wallets/${jsonFileWallet.id}/transactions/${jsonFileCreatedTransactionResponse.id}", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("transaction.json"))

        case (r"wallets/.+/transactions.start=.+", HttpMethods.GET) =>
          val transactions = getTransactions(
            walletId = jsonFileWallet.id,
            start = parseZonedDT("start"),
            end = parseZonedDT("end"),
            order = Order.withName(query("order")),
            minWithdrawal = query("minWithdrawal").toInt
          )
          toJsonResponse(transactions)

        case (s"wallets/${jsonFileWallet.id}/transactions", HttpMethods.POST) =>
          request.mapper(httpEntityFromJson("transaction.json"))

        case (s"wallets/${jsonFileWallet.id}/payment-fees", HttpMethods.POST) =>
          request.mapper(httpEntityFromJson("estimate_fees.json"))

        case (s"wallets/${jsonFileWallet.id}/coin-selections/random", HttpMethods.POST) =>
          request.mapper(httpEntityFromJson("coin_selections_random.json"))

        case (r"wallets/.+/transactions/.+", HttpMethods.GET) => notFound("Transaction not found")
        case (r"wallets/.+", _)                               => notFound("Wallet not found")
        case _                                                => notFound("Not found")
      }

    }
  }
}
