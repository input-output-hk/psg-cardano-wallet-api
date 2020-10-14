package iog.psg.cardano.util

import java.io.File
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{ parser, Json }
import iog.psg.cardano.CardanoApi.Order.Order
import iog.psg.cardano.CardanoApi.{ CardanoApiRequest, CardanoApiResponse, ErrorMessage, Order }
import iog.psg.cardano.CardanoApiCodec.{ GenericMnemonicSecondaryFactor, GenericMnemonicSentence }
import iog.psg.cardano.jpi.CardanoApiException
import iog.psg.cardano.{ ApiRequestExecutor, CardanoApi }
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ ExecutionContext, Future }

trait InMemoryCardanoApi {
  this: ScalaFutures with Assertions with JsonFiles with DummyModel =>

  protected val postWalletFieldsToCheck: List[String] = List("name", "passphrase", "mnemonic_sentence", "mnemonic_second_factor", "address_pool_gap")

  implicit val as: ActorSystem
  implicit lazy val ec = as.dispatcher

  final val baseUrl: String = "http://fake:1234/"

  private implicit final class RegexOps(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  implicit final class InMemoryExecutor[T](req: CardanoApiRequest[T]) {
    def executeOrFail(): T =
      inMemoryExecutor.execute(req).futureValue match {
        case Left(value)  => fail(s"Request failed: ${value.message}")
        case Right(value) => value
      }

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
  ) =
    HttpResponse(entity = {
      val resource = getClass.getResource(s"/jsons/$jsonFileName")
      val file = new File(resource.getFile)
      HttpEntity.fromFile(contentType, file)
    })

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
    }.sortWith((ta, tb) => if (order.toString == Order.descendingOrder.toString) ta.id > tb.id else ta.id < tb.id)

  val inMemoryExecutor: ApiRequestExecutor = new ApiRequestExecutor {
    override def execute[T](
      request: CardanoApi.CardanoApiRequest[T]
    )(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {

      implicit class JsonRespectable[A](resp: A) {
        def toJsonResponse()(implicit enc: io.circe.Encoder[A]): Future[CardanoApiResponse[T]] =
          request.mapper(
            HttpResponse(entity =
              HttpEntity(resp.asJson.noSpaces)
                .withContentType(ContentType.WithFixedCharset(MediaTypes.`application/json`))
            )
          )
      }

      val apiAddress = request.request.uri.toString().split(baseUrl).lastOption.getOrElse("")
      val method = request.request.method
      lazy val query = request.request.uri.query().toMap

      def notFound(msg: String): Future[CardanoApiResponse[T]] = {
        val json: String = ErrorMessage(msg, "404").asJson.noSpaces
        val entity = HttpEntity(json)
        request.mapper(HttpResponse(status = StatusCodes.NotFound, entity = entity))
      }

      def unmarshalJsonBody(): Future[Json] =
        Unmarshal(request.request.entity)
          .to[String]
          .map(str => parser.parse(str).getOrElse(fail("Could not parse json body")))

      def checkIfContainsProperJsonKeys(json: Json, expectedList: List[String]): Future[Unit] = {
        val missingFields = expectedList.diff(json.dropNullValues.hcursor.keys.getOrElse(Nil).toList)
        if (missingFields.isEmpty)
          Future.successful(())
        else
          Future.failed(new CardanoApiException(s"Invalid json body, missing fields: ${missingFields.mkString(", ")}", "400"))
      }

      def getQueryZonedDTParam(name: String): ZonedDateTime = ZonedDateTime.parse(query(name))

      def checkValueOrFail[A](jsonValue: A, expectedValue: A, fieldName: String): Future[Unit] =
        if (jsonValue == expectedValue) Future.successful(())
        else Future.failed(new CardanoApiException(s"Invalid $fieldName", "400"))

      def getAsString(json: Json, field: String): String = json.\\(field).headOption.flatMap(_.asString).get

      def getAsMnemonicString(json: Json, field: String): String =
        json.\\(field).headOption.flatMap(_.asArray).get.flatMap(_.asString).mkString(" ")

      def checkPaymentsField(json: Json): Future[Unit] = {
        val jsonPayments = json.\\("payments")
        val jsonAddresses = jsonPayments.flatMap(_.\\("address")).flatMap(_.asString)
        for {
          _ <- checkValueOrFail(jsonAddresses, payments.payments.map(_.address), "payments.address")
          jsonAmount = jsonPayments.flatMap(_.\\("amount"))
          jsonAmountQuantities = jsonAmount.flatMap(_.\\("quantity").flatMap(_.asNumber.flatMap(_.toLong)))
          _ <- checkValueOrFail(jsonAmountQuantities, payments.payments.map(_.amount.quantity), "amount.quantity")
          jsonAmountUnits = jsonAmount.flatMap(_.\\("unit").flatMap(_.asString))
          _ <- checkValueOrFail(jsonAmountUnits, payments.payments.map(_.amount.unit.toString), "amount.unit")
        } yield ()
      }

      def checkStringField(json: Json, jsonFieldName: String, expectedValue: String): Future[Unit] = {
        val jsonValueStr = getAsString(json, jsonFieldName)
        checkValueOrFail(jsonValueStr, expectedValue, jsonFieldName)
      }

      def checkPassphraseField(json: Json): Future[Unit] =
        checkStringField(json, "passphrase", walletPassphrase)

      def checkWithdrawalField(json: Json): Future[Unit] =
        checkStringField(json, "withdrawal", withdrawal)

      def checkMetadataField(json: Json): Future[Unit] =
        for {
          metadata <- {
            val metaResults = json.\\("metadata")
            if (metaResults.isEmpty) Future.failed(new CardanoApiException(s"Invalid metadata", "400"))
            else Future.successful(json.\\("metadata").head)
          }
          metadataKeys = metadata.hcursor.keys.getOrElse(Nil)
          _ <- checkValueOrFail(metadataKeys.toList, metadataMap.toList.map(_._1.toString), "metadata.keys")
          metadataValues = for {
            key <- metadataKeys.toList
            value <- metadata.\\(key).flatMap { keyField =>
              keyField.\\("string").flatMap(_.asString)
            }
          } yield value
          _ <- checkValueOrFail(metadataValues, metadataMap.map(_._2.s), "metadata.values")
        } yield ()

      (apiAddress, method) match {
        case ("network/information", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("netinfo.json"))

        case ("wallets", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("wallets.json"))

        case ("wallets", HttpMethods.POST) =>
          println("postWalletFieldsToCheck: "+postWalletFieldsToCheck)
          for {
            json <- unmarshalJsonBody()
            _ <- checkIfContainsProperJsonKeys(json, postWalletFieldsToCheck)
            _ <- checkPassphraseField(json)
            jsonMnemonicSentence = GenericMnemonicSentence(getAsMnemonicString(json, "mnemonic_sentence"))
            _ <- checkValueOrFail(jsonMnemonicSentence, mnemonicSentence, "mnemonic_sentence")
            _ <- {
              val fieldName = "mnemonic_second_factor"
              println("postWalletFieldsToCheck.contains(fieldName): "+postWalletFieldsToCheck.contains(fieldName))
              if (postWalletFieldsToCheck.contains(fieldName)) {
                val jsonMnemonicSecondFactor = GenericMnemonicSecondaryFactor(getAsMnemonicString(json, fieldName))
                checkValueOrFail(jsonMnemonicSecondFactor, mnemonicSecondFactor, fieldName)
              } else {
                Future.successful(())
              }
            }
            jsonName = getAsString(json, "name")
            jsonAddressPoolGap = json.\\("address_pool_gap").headOption.flatMap(_.asNumber).get.toInt.get
            response <- wallet.copy(name = jsonName, addressPoolGap = jsonAddressPoolGap).toJsonResponse()
          } yield response

        case (s"wallets/${jsonFileWallet.id}", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("wallet.json"))

        case (s"wallets/${jsonFileWallet.id}", HttpMethods.DELETE) =>
          request.mapper(HttpResponse(status = StatusCodes.NoContent))

        case (s"wallets/${jsonFileWallet.id}/passphrase", HttpMethods.PUT) =>
          for {
            json     <- unmarshalJsonBody()
            _        <- checkStringField(json, "old_passphrase", oldPassword)
            _        <- checkStringField(json, "new_passphrase", newPassword)
            response <- request.mapper(HttpResponse(status = StatusCodes.NoContent))
          } yield response

        case (s"wallets/${jsonFileWallet.id}/addresses?state=unused", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("unused_addresses.json"))

        case (s"wallets/${jsonFileWallet.id}/addresses?state=used", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("used_addresses.json"))

        case (s"wallets/${jsonFileWallet.id}/addresses", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("addresses.json"))

        case (s"wallets/${jsonFileWallet.id}/transactions?order=descending", HttpMethods.GET) =>
          jsonFileCreatedTransactionsResponse.sortWith(_.id > _.id).toJsonResponse()

        case (s"wallets/${jsonFileWallet.id}/transactions/${jsonFileCreatedTransactionResponse.id}", HttpMethods.GET) =>
          request.mapper(httpEntityFromJson("transaction.json"))

        case (r"wallets/.+/transactions.start=.+", HttpMethods.GET) =>
          getTransactions(
            walletId = jsonFileWallet.id,
            start = getQueryZonedDTParam("start"),
            end = getQueryZonedDTParam("end"),
            order = Order.withName(query("order")),
            minWithdrawal = query("minWithdrawal").toInt
          ).toJsonResponse()

        case (s"wallets/${jsonFileWallet.id}/transactions", HttpMethods.POST) =>
          for {
            jsonBody <- unmarshalJsonBody()
            _        <- checkIfContainsProperJsonKeys(jsonBody, List("passphrase", "payments", "metadata", "withdrawal"))
            _        <- checkPassphraseField(jsonBody)
            _        <- checkPaymentsField(jsonBody)
            _        <- checkMetadataField(jsonBody)
            _        <- checkWithdrawalField(jsonBody)
            response <- request.mapper(httpEntityFromJson("transaction.json"))
          } yield response

        case (s"wallets/${jsonFileWallet.id}/payment-fees", HttpMethods.POST) =>
          for {
            jsonBody <- unmarshalJsonBody()
            _        <- checkIfContainsProperJsonKeys(jsonBody, List("payments", "withdrawal", "metadata"))
            _        <- checkPaymentsField(jsonBody)
            _        <- checkMetadataField(jsonBody)
            _        <- checkWithdrawalField(jsonBody)
            response <- request.mapper(httpEntityFromJson("estimate_fees.json"))
          } yield response

        case (s"wallets/${jsonFileWallet.id}/coin-selections/random", HttpMethods.POST) =>
          request.mapper(httpEntityFromJson("coin_selections_random.json"))

        case (r"wallets/.+/transactions/.+", HttpMethods.GET) => notFound("Transaction not found")
        case (r"wallets/.+", _)                               => notFound("Wallet not found")
        case _                                                => notFound("Not found")
      }

    }
  }
}
