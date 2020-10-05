package iog.psg.cardano


import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import iog.psg.cardano.CardanoApi.Order.Order
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Defines the API which wraps the Cardano API, depends on CardanoApiCodec for it's implementation,
 * so clients will import the Codec also.
 */
object CardanoApi {

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

    implicit class FlattenOp[T](val knot: Future[CardanoApiResponse[Future[CardanoApiResponse[T]]]]) extends AnyVal {

      def flattenCardanoApiResponse(implicit ec: ExecutionContext): Future[CardanoApiResponse[T]] = knot.flatMap {
        case Left(errorMessage) => Future.successful(Left(errorMessage))
        case Right(vaue) => vaue
      }
    }

    implicit class FutOp[T](val request: CardanoApiRequest[T]) extends AnyVal {
      def toFuture: Future[CardanoApiRequest[T]] = Future.successful(request)
    }

    //tie execute to ioEc
    implicit class CardanoApiRequestFOps[T](requestF: Future[CardanoApiRequest[T]])(implicit executor: ApiRequestExecutor, ec: ExecutionContext, as: ActorSystem) {
      def execute: Future[CardanoApiResponse[T]] = {
        requestF.flatMap(_.execute)
      }

      def executeBlocking(implicit maxWaitTime: Duration): CardanoApiResponse[T] =
        Await.result(execute, maxWaitTime)

    }

    implicit class CardanoApiRequestOps[T](request: CardanoApiRequest[T])(implicit executor: ApiRequestExecutor, ec: ExecutionContext, as: ActorSystem) {

      def execute: Future[CardanoApiResponse[T]] = executor.execute(request)

      def executeBlocking(implicit maxWaitTime: Duration): CardanoApiResponse[T] =
        Await.result(execute, maxWaitTime)
    }

  }

}

class CardanoApi(baseUriWithPort: String)(implicit ec: ExecutionContext, as: ActorSystem) {

  import iog.psg.cardano.CardanoApi._
  import iog.psg.cardano.CardanoApiCodec._
  import AddressFilter.AddressFilter

  private val wallets = s"${baseUriWithPort}wallets"
  private val network = s"${baseUriWithPort}network"

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  /**
   * List of known wallets, ordered from oldest to newest.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listWallets #listWallets]]
   *
   * @return list wallets request
   */
  def listWallets: CardanoApiRequest[Seq[Wallet]] = CardanoApiRequest(
    HttpRequest(
      uri = wallets,
      method = GET
    ),
    _.toWallets
  )

  /**
   * Get wallet details by id
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet #getWallet]]
   *
   * @param walletId wallet's id
   * @return get wallet request
   */
  def getWallet(walletId: String): CardanoApiRequest[Wallet] = CardanoApiRequest(
    HttpRequest(
      uri = s"${wallets}/$walletId",
      method = GET
    ),
    _.toWallet
  )

  /**
   * Gives network information
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkInformation #getNetworkInformation]]
   *
   * @return network info request
   */
  def networkInfo: CardanoApiRequest[NetworkInfo] = CardanoApiRequest(
    HttpRequest(
      uri = s"${network}/information",
      method = GET
    ),
    _.toNetworkInfoResponse
  )

  /**
   * Create and restore a wallet from a mnemonic sentence or account public key.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet #postWallet]]
   *
   * @param name wallet's name
   * @param passphrase A master passphrase to lock and protect the wallet for sensitive operation (e.g. sending funds)
   * @param mnemonicSentence A list of mnemonic words [ 15 .. 24 ] items ( can be generated using https://iancoleman.io/bip39 )
   * @param mnemonicSecondFactor An optional passphrase used to encrypt the mnemonic sentence. [ 9 .. 12 ] items
   * @param addressPoolGap An optional number of consecutive unused addresses allowed
   * @return create/restore wallet request
   */
  def createRestoreWallet(
                           name: String,
                           passphrase: String,
                           mnemonicSentence: MnemonicSentence,
                           mnemonicSecondFactor: Option[MnemonicSentence] = None, //TODO write tests scala+java
                           addressPoolGap: Option[Int] = None
                         ): Future[CardanoApiRequest[Wallet]] = {

    val createRestore =
      CreateRestore(
        name,
        passphrase,
        mnemonicSentence.mnemonicSentence,
        mnemonicSecondFactor.map(_.mnemonicSentence),
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

  /**
   * List of known addresses, ordered from newest to oldest
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Addresses #Addresses]]
   *
   * @param walletId wallet's id
   * @param state addresses state: used, unused
   * @return list wallet addresses request
   */
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

  /**
   * Lists all incoming and outgoing wallet's transactions.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listTransactions #listTransactions]]
   *
   * @param walletId wallet's id
   * @param start    An optional start time in ISO 8601 date-and-time format. Basic and extended formats are both accepted. Times can be local (with a timezone offset) or UTC.
   *                 If both a start time and an end time are specified, then the start time must not be later than the end time.
   *                 Example: 2008-08-08T08:08:08Z
   * @param end      An optional end time in ISO 8601 date-and-time format. Basic and extended formats are both accepted. Times can be local (with a timezone offset) or UTC.
   *                 If both a start time and an end time are specified, then the start time must not be later than the end time.
   *                 Example: 2008-08-08T08:08:08Z
   * @param order    Default: "descending" ( "ascending", "descending" )
   * @param minWithdrawal Returns only transactions that have at least one withdrawal above the given amount.
   *                      This is particularly useful when set to 1 in order to list the withdrawal history of a wallet.
   * @return list wallet's transactions request
   */
  def listTransactions(walletId: String,
                       start: Option[ZonedDateTime] = None,
                       end: Option[ZonedDateTime] = None,
                       order: Order = Order.descendingOrder,
                       minWithdrawal: Option[Int] = None): CardanoApiRequest[Seq[CreateTransactionResponse]] = {
    val baseUri = Uri(s"${wallets}/${walletId}/transactions")

    val queries =
      Seq("start", "end", "order", "minWithdrawal").zip(Seq(start, end, order, minWithdrawal))
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

  /**
   * Create and send transaction from the wallet.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction #postTransaction]]
   *
   * @param fromWalletId wallet's id
   * @param passphrase The wallet's master passphrase. [ 0 .. 255 ] characters
   * @param payments A list of target outputs ( address, amount )
   * @param withdrawal Optional, when provided, instruments the server to automatically withdraw rewards from the source
   *                   wallet when they are deemed sufficient (i.e. they contribute to the balance for at least as much
   *                   as they cost).
   * @param metadata   Extra application data attached to the transaction.
   * @return create transaction request
   */
  def createTransaction(fromWalletId: String,
                        passphrase: String,
                        payments: Payments,
                        metadata: Option[TxMetadataIn],
                        withdrawal: Option[String]
                       ): Future[CardanoApiRequest[CreateTransactionResponse]] = {


    val createTx = CreateTransaction(passphrase, payments.payments, metadata, withdrawal)

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

  /**
   * Estimate fee for the transaction. The estimate is made by assembling multiple transactions and analyzing the
   * distribution of their fees. The estimated_max is the highest fee observed, and the estimated_min is the fee which
   * is lower than at least 90% of the fees observed.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee #estimateFee]]
   *
   * @param fromWalletId wallet's id
   * @param payments A list of target outputs ( address, amount )
   * @param withdrawal Optional, when provided, instruments the server to automatically withdraw rewards from the source
   *                   wallet when they are deemed sufficient (i.e. they contribute to the balance for at least as much
   *                   as they cost).
   * @param metadataIn Extra application data attached to the transaction.
   * @return estimate fee request
   */
  def estimateFee(fromWalletId: String,
                  payments: Payments,
                  withdrawal: String = "self",
                  metadataIn: Option[TxMetadataIn] = None //TODO add to request
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

  /**
   * Select coins to cover the given set of payments.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#tag/Coin-Selections #CoinSelections]]
   *
   * @param walletId wallet's id
   * @param payments A list of target outputs ( address, amount )
   * @return fund payments request
   */
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

  /**
   * Get transaction by id.
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction #getTransaction]]
   *
   * @param walletId wallet's id
   * @param transactionId transaction's id
   * @return get transaction request
   */
  def getTransaction[T <: TxMetadataIn](
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

  /**
   * Update Passphrase
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWalletPassphrase #putWalletPassphrase]]
   * @param walletId wallet's id
   * @param oldPassphrase current passphrase
   * @param newPassphrase new passphrase
   * @return update passphrase request
   */
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

  /**
   * Delete wallet by id
   * Api Url: [[https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteWallet #deleteWallet]]
   * @param walletId wallet's id
   * @return delete wallet request
   */
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
