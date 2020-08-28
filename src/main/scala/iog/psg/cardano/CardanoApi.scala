package iog.psg.cardano


import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import iog.psg.cardano.CardanoApi.IOExecutionContext
import iog.psg.cardano.CardanoApi.Order.Order

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Defines the API which wraps the Cardano API, depends on CardanoApiCodec for it's implementation,
 * so clients will import the Codec also.
 */
object CardanoApi {

  //Marker class, differenciate between multiple ec's in a client
  case class IOExecutionContext(ec: ExecutionContext) {
    implicit val ioEc: ExecutionContext = ec
  }

  case class ErrorMessage(message: String, code: String)

  type CardanoApiResponse[T] = Either[ErrorMessage, T]

  case class CardanoApiRequest[T](request: HttpRequest, mapper: HttpResponse => Future[CardanoApiResponse[T]])

  object Order extends Enumeration {
    type Order = Value
    val ascendingOrder = Value("ascending")
    val descendingOrder = Value("descending")
  }

  implicit val defaultMaxWaitTime: FiniteDuration = 15.seconds

  object CardanoApiOps {

    implicit class TransformOp[T](val knot: Future[CardanoApiResponse[Future[CardanoApiResponse[T]]]]) extends AnyVal {
      //Cannot call this transform as it clashes with futire
      def unknot(implicit ec: ExecutionContext): Future[CardanoApiResponse[T]] = knot.flatMap {
        case Left(errorMessage) => Future.successful(Left(errorMessage))
        case Right(vaue) => vaue
      }
    }

    implicit class FutOp[T](val request: CardanoApiRequest[T]) extends AnyVal {
      def toFuture: Future[CardanoApiRequest[T]] = Future.successful(request)
    }


  }

}

class CardanoApi(baseUriWithPort: String)(implicit ec: IOExecutionContext, as: ActorSystem) {

  import iog.psg.cardano.CardanoApi._
  import iog.psg.cardano.CardanoApiCodec._
  import AddressFilter.AddressFilter
  import ec.ioEc

  private val wallets = s"${baseUriWithPort}wallets"
  private val network = s"${baseUriWithPort}network"

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames



  object Ops {
    //tie execute to ioEc
    implicit class CardanoApiRequestFOps[T](requestF: Future[CardanoApiRequest[T]]) {
      def execute: Future[CardanoApiResponse[T]] = {
        requestF.flatMap(_.execute)
      }

      def executeBlocking(implicit maxWaitTime: Duration): CardanoApiResponse[T] =
        Await.result(execute, maxWaitTime)

    }

    implicit class CardanoApiRequestOps[T](request: CardanoApiRequest[T]) {
      def execute: Future[CardanoApiResponse[T]] = {
          Http()
            .singleRequest(request.request)
            .flatMap(request.mapper)
        }

      def executeBlocking(implicit maxWaitTime: Duration): CardanoApiResponse[T] =
        Await.result(execute, maxWaitTime)
    }

  }
  def listWallets: CardanoApiRequest[Seq[Wallet]] = CardanoApiRequest(
    HttpRequest(
      uri = wallets,
      method = GET
    ),
    _.toWallets
  )

  def getWallet(walletId: String): CardanoApiRequest[Wallet] = CardanoApiRequest(
    HttpRequest(
      uri = s"${wallets}/$walletId",
      method = GET
    ),
    _.toWallet
  )

  def networkInfo: CardanoApiRequest[NetworkInfo] = CardanoApiRequest(
    HttpRequest(
      uri = s"${network}/information",
      method = GET
    ),
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
          uri = s"$wallets",
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
      baseUri.withQuery(Query("state" -> s.toString))
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
                       start: Option[ZonedDateTime] = None,
                       end: Option[ZonedDateTime] = None,
                       order: Order = Order.descendingOrder,
                       minWithdrawal: Int = 1): CardanoApiRequest[Seq[CreateTransactionResponse]] = {
    val baseUri = Uri(s"${wallets}/${walletId}/transactions")

    val queries =
      Seq("start", "end", "order", "minWithdrawal").zip(Seq(start, end, order, Some(minWithdrawal)))
        .collect {
          case (queryParamName, Some(o: Order)) => queryParamName -> o.toString
          case (queryParamName, Some(dt: ZonedDateTime)) => queryParamName -> zonedDateToString(dt)
          case (queryParamName, Some(minWith: Int)) => queryParamName -> minWith.toString
        }


    val uriWithQueries = baseUri.withQuery(Query(queries: _*))

    CardanoApiRequest(
      HttpRequest(
        uri = uriWithQueries,
        method = GET
      ),
      _.toCreateTransactionResponses
    )
  }

  def createTransaction(fromWalletId: String,
                        passphrase: String,
                        payments: Payments,
                        withdrawal: Option[String]
                       ): Future[CardanoApiRequest[CreateTransactionResponse]] = {

    val createTx = CreateTransaction(passphrase, payments.payments, withdrawal)

    Marshal(createTx).to[RequestEntity] map { marshalled =>
      CardanoApiRequest(
        HttpRequest(
          uri = s"${wallets}/${fromWalletId}/transactions",
          method = POST,
          entity = marshalled
        ),
        _.toCreateTransactionResponse
      )
    }
  }

  def estimateFee(fromWalletId: String,
                        payments: Payments,
                        withdrawal: String = "self"
                       ): Future[CardanoApiRequest[EstimateFeeResponse]] = {

    val estimateFees = EstimateFee(payments.payments, withdrawal)

    Marshal(estimateFees).to[RequestEntity] map { marshalled =>
      CardanoApiRequest(
        HttpRequest(
          uri = s"${wallets}/${fromWalletId}/payment-fees",
          method = POST,
          entity = marshalled
        ),
        _.toEstimateFeeResponse
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

  def updatePassphrase(
                        walletId: String,
                        oldPassphrase: String,
                        newPassphrase: String): Future[CardanoApiRequest[Unit]] = {

      val uri = Uri(s"${wallets}/${walletId}/passphrase")
      val updater = UpdatePassphrase(oldPassphrase, newPassphrase)

      Marshal(updater).to[RequestEntity] map { marshalled => {
        CardanoApiRequest(
          HttpRequest(
            uri = uri,
            method = PUT,
            entity = marshalled
          ),
          _.toUnit
        )
      }
    }
  }

  def deleteWallet(
                    walletId: String
                  ): CardanoApiRequest[Unit] = {

    val uri = Uri(s"${wallets}/${walletId}")

    CardanoApiRequest(
      HttpRequest(
        uri = uri,
        method = DELETE,
      ),
      _.toUnit
    )
  }

}
