package iog.psg.cardano


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, DescendingOrder, Order}
import iog.psg.cardano.CardanoApiCodec._

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

object CardanoApi {

  type CardanoApiResponse[T] = Either[ErrorMessage, T]

  case class CardanoApiRequest[T](request: HttpRequest, entityMapper: HttpEntity.Strict => Future[CardanoApiResponse[T]])

  sealed trait Order {
    val value: String
  }

  object AscendingOrder extends Order {
    override val value: String = "ascending"
  }

  object DescendingOrder extends Order {
    override val value: String = "descending"
  }

  implicit val maxWaitTime: FiniteDuration = 1.minute

  object CardanoApiOps {

    implicit class FutOp[T](val request: CardanoApiRequest[T]) extends AnyVal {
      def toFuture: Future[CardanoApiRequest[T]] = Future.successful(request)
    }

    implicit class CardanoApiRequestOps[T](requestF: Future[CardanoApiRequest[T]])
                                          (implicit ec: ExecutionContext,
                                           as: ActorSystem,
                                           maxToStrictWaitTime: FiniteDuration
                                          ) {
      def execute: Future[CardanoApiResponse[T]] = {
        requestF flatMap { request =>
          Http().singleRequest(request.request) flatMap { response =>
            if (response.status == StatusCodes.Forbidden) {
              Future.successful(Left(ErrorMessage("Status code 'Forbidden' causes decoding error due to octet content type", StatusCodes.Forbidden.value)))
            } else {
              // Load into memory using toStrict
              // a. no responses utilise streaming and
              // b. the Either unmarshaller requires it
              response.entity.toStrict(maxToStrictWaitTime) flatMap { strictEntity =>
                request.entityMapper(strictEntity)
              }
            }
          }
        }
      }

      def executeBlocking(implicit maxWaitTime: Duration): Try[CardanoApiResponse[T]] = Try {
        Await.result(
          execute,
          maxWaitTime
        )
      }
    }

  }

}

class CardanoApi(baseUriWithPort: String)(implicit ec: ExecutionContext, as: ActorSystem) {

  private val wallets = s"${baseUriWithPort}wallets"
  private val network = s"${baseUriWithPort}network"

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames


  def listWallets: CardanoApiRequest[Seq[Wallet]] = CardanoApiRequest(
    HttpRequest(uri = wallets),
    _.toWallets
  )

  def networkInfo: CardanoApiRequest[NetworkInfo] = CardanoApiRequest(
    HttpRequest(uri = s"${network}/information"),
    _.toNetworkInfoResponse
  )

  def createRestoreWallet(
                           name: String,
                           passphrase: String,
                           mnemonicSentence: MnemonicSentence,
                           addressPoolGap: Option[Int] = None

                         ): Future[CardanoApiRequest[Wallet]] = {

    val createRestore =
      CreateRestore(
        name,
        passphrase,
        mnemonicSentence.mnemonicSentence,
        addressPoolGap
      )

    Marshal(createRestore).to[RequestEntity].map { marshalled =>
      CardanoApiRequest(
        HttpRequest(
          uri = s"${baseUriWithPort}wallets",
          method = POST,
          entity = marshalled
        ),
        _.toWallet
      )
    }

  }

  def listAddresses(walletId: String,
                    state: Option[AddressFilter]): CardanoApiRequest[Seq[WalletAddressId]] = {

    val baseUri = Uri(s"${wallets}/${walletId}/addresses")
    val url = state.map { s =>
      baseUri.withQuery(Query("state" -> s))
    }.getOrElse(baseUri)

    CardanoApiRequest(
      HttpRequest(
        uri = url,
        method = GET
      ),
      _.toWalletAddressIds
    )

  }

  def listTransactions(walletId: String,
                       start: Option[DateTime] = None,
                       end: Option[DateTime] = None,
                       order: Order = DescendingOrder,
                       minWithdrawal: Int = 1): CardanoApiRequest[Seq[Transaction]] = {
    val baseUri = Uri(s"${wallets}/${walletId}/transactions")

    val queries =
      Seq("start", "end", "order", "minWithdrawal").zip(Seq(start, end, order, Some(minWithdrawal)))
        .collect {
          case (queryParamName, Some(dt: DateTime)) => queryParamName -> dt.toIsoDateTimeString()
          case (queryParamName, Some(minWith: Int)) => queryParamName -> minWith.toString
        }

    val uriWithQueries = baseUri.withQuery(Query(queries: _*))
    CardanoApiRequest(
      HttpRequest(
        uri = uriWithQueries,
        method = GET
      ),
      _.toWalletTransactions
    )
  }

  def createTransaction(walletId: String,
                       passphrase: String,
                       payments: Payments,
                       withdrawal: Option[String]
                       ): Future[CardanoApiRequest[CreateTransactionResponse]] = {

    val createTx = CreateTransaction(passphrase, payments.payments, withdrawal)

    Marshal(createTx).to[RequestEntity] map { marshalled =>
      CardanoApiRequest(
        HttpRequest(
          uri = s"${wallets}/${walletId}/transactions",
          method = POST,
          entity = marshalled
        ),
        _.toCreateTransactionResponse
      )
    }
  }

  def fundPayments(walletId: String,
                   payments: Payments): Future[CardanoApiRequest[FundPaymentsResponse]] = {
    Marshal(payments).to[RequestEntity] map { marshalled =>
      CardanoApiRequest(
        HttpRequest(
          uri = s"${wallets}/${walletId}/coin-selections/random",
          method = POST,
          entity = marshalled
        ),
        _.toFundPaymentsResponse
      )
    }
  }

  def getTransaction(
                      walletId: String,
                      transactionId: String): CardanoApiRequest[CreateTransactionResponse] = {

    val uri = Uri(s"${wallets}/${walletId}/transactions/${transactionId}")

    CardanoApiRequest(
      HttpRequest(
        uri = uri,
        method = GET
      ),
      _.toCreateTransactionResponse
    )
  }

}
