package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.codecs.CardanoApiCodec.AddressFilter
import iog.psg.cardano.util.{CustomPatienceConfiguration, DummyModel, InMemoryCardanoApi, JsonFiles, ModelCompare}
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

  lazy val api = new CardanoApi(baseUrl)

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
    api.createRestoreWallet(wallet.name, "Pass9128!", mnemonicSentence).executeOrFail() shouldBe wallet
  }

  "GET /wallets/{walletId}/addresses?state=unused" should "return wallet's unused addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.unUsed)).executeOrFail().map(_.id) shouldBe unUsedAddresses.map(
      _.id
    )
  }

  it should "return wallet's used addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.used)).executeOrFail().map(_.id) shouldBe usedAddresses.map(_.id)
  }

  it should "return wallet not found error" in {
    api
      .listAddresses("invalid_wallet_id", Some(AddressFilter.used))
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/transactions" should "return wallet's transactions" in {
    api.listTransactions(wallet.id).executeOrFail().map(_.id) shouldBe Seq(createdTransactionResponse.id)
  }

  it should "return not found error" in {
    api.listTransactions("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/transactions/{transactionId}" should "return transaction" in {
    api
      .getTransaction(wallet.id, createdTransactionResponse.id)
      .executeOrFail()
      .id shouldBe createdTransactionResponse.id
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
        passphrase = "MySecret",
        payments = payments,
        metadata = None,
        withdrawal = None
      )
      .executeOrFail()
      .id shouldBe createdTransactionResponse.id
  }

  it should "return not found" in {
    api
      .createTransaction(
        fromWalletId = "invalid_wallet_id",
        passphrase = "MySecret",
        payments = payments,
        metadata = None,
        withdrawal = None
      )
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "POST /wallets/{fromWalletId}/payment-fees" should "estimate fee" in {
    api.estimateFee(wallet.id, payments, None).executeOrFail() shouldBe estimateFeeResponse
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
    api.updatePassphrase(wallet.id, "old_password", "new_password").executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api
      .updatePassphrase("invalid_wallet_id", "old_password", "new_password")
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
