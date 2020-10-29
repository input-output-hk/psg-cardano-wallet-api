package iog.psg.cardano

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.CardanoApiCodec.AddressFilter
import iog.psg.cardano.util._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiSpec
    extends AnyFlatSpec
    with Matchers
    with ModelCompare
    with ScalaFutures
    with InMemoryCardanoApi
    with DummyModel
    with JsonFiles
    with CustomPatienceConfiguration {

  lazy val api = CardanoApi(baseUrl)

  private val walletNotFoundError = ErrorMessage(s"Wallet not found", "404")

  "GET /wallets" should "return wallets list" in {
    api.listWallets.executeOrFail().head shouldBe wallet
  }

  "GET /wallets/{walletId}" should "return existing wallet" in {
    api.getWallet(wallet.id).executeOrFail() shouldBe wallet
  }

  it should "return 404 if wallet does not exists" in {
    api.getWallet("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /network/information" should "return network information" in {
    api.networkInfo.executeOrFail() shouldBe networkInfo
  }

  "POST /wallets" should "" in {
    api
      .createRestoreWallet(randomWalletName, walletPassphrase, mnemonicSentence, Some(mnemonicSecondFactor), Some(addressPoolGap))
      .executeOrFail() shouldBe wallet.copy(name = randomWalletName, addressPoolGap = addressPoolGap)
  }

  "GET /wallets/{walletId}/addresses?state=unused" should "return wallet's unused addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.unUsed)).executeOrFail().map(_.id) shouldBe unUsedAddresses.map(
      _.id
    )
  }

  it should "return wallet's used addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.used)).executeOrFail().map(_.id) shouldBe usedAddresses.map(_.id)
  }

  it should "return wallet's used + unused addresses" in {
    api.listAddresses(wallet.id, None).executeOrFail().map(_.id) shouldBe addresses.map(_.id)
  }

  it should "return wallet not found error" in {
    api
      .listAddresses("invalid_wallet_id", Some(AddressFilter.used))
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/transactions" should "return wallet's transactions" in {
    val transactions = api.listTransactions(wallet.id).executeOrFail()
    transactions.map(_.id) shouldBe transactionsIdsDesc
  }

  it should "run request with proper params" in {
    val start = ZonedDateTime.parse("2000-01-01T00:00:00.000Z")
    val end = ZonedDateTime.parse("2001-01-01T00:00:00.000Z")
    val transactions = api
      .listTransactions(
        wallet.id,
        start = Some(start),
        end = Some(end),
        order = CardanoApi.Order.ascendingOrder,
        minWithdrawal = Some(100)
      )
      .executeOrFail()
    transactions.map(_.id) shouldBe oldTransactionsIdsAsc
  }

  it should "return not found error" in {
    api.listTransactions("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/transactions/{transactionId}" should "return transaction" in {
    api
      .getTransaction(wallet.id, firstTransactionId)
      .executeOrFail()
      .id shouldBe firstTransactionId
  }

  it should "return not found error" in {
    api.getTransaction(wallet.id, "not_existing_id").executeExpectingErrorOrFail() shouldBe ErrorMessage(
      "Transaction not found",
      "404"
    )
  }

  "POST /wallets/{walletId}/transactions" should "create transaction" in {
    api
      .createTransaction(
        fromWalletId = wallet.id,
        passphrase = walletPassphrase,
        payments = payments,
        metadata = Some(txMetadata),
        withdrawal = Some(withdrawal)
      )
      .executeOrFail()
      .id shouldBe firstTransactionId
  }

  it should "return not found" in {
    api
      .createTransaction(
        fromWalletId = "invalid_wallet_id",
        passphrase = "MySecret",
        payments = payments,
        metadata = Some(txMetadata),
        withdrawal = Some(withdrawal)
      )
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "POST /wallets/{fromWalletId}/payment-fees" should "estimate fee" in {
    api.estimateFee(wallet.id, payments, Some(withdrawal), Some(txMetadata)).executeOrFail() shouldBe estimateFeeResponse
  }

  it should "return not found" in {
    api.estimateFee("invalid_wallet_id", payments, None).executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "POST /wallets/{walletId}/coin-selections/random" should "fund payments" in {
    api.fundPayments(wallet.id, payments).executeOrFail() shouldBe fundPaymentsResponse
  }

  it should "return not found" in {
    api.fundPayments("invalid_wallet_id", payments).executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "PUT /wallets/{walletId/passphrase" should "update passphrase" in {
    api.updatePassphrase(wallet.id, oldPassword, newPassword).executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api
      .updatePassphrase("invalid_wallet_id", oldPassword, newPassword)
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "DELETE /wallets/{walletId" should "delete wallet" in {
    api.deleteWallet(wallet.id).executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api.deleteWallet("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

}
