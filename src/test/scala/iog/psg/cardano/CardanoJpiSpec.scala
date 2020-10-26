package iog.psg.cardano

import java.time.ZonedDateTime
import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import iog.psg.cardano.jpi.{AddressFilter, JpiResponseCheck, ListTransactionsParamBuilder}
import iog.psg.cardano.util._
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._
import scala.util.Try

class CardanoJpiSpec
  extends AnyFlatSpec
    with Matchers
    with Configure
    with ModelCompare
    with ScalaFutures
    with InMemoryCardanoApi
    with DummyModel
    with JsonFiles {

  lazy val api = JpiResponseCheck.buildWithPredefinedApiExecutor(inMemoryExecutor, as)

  private def tryGetErrorMessage[T](completionStage: CompletionStage[T]) =
    Try(completionStage.toCompletableFuture.get()).toEither.swap.getOrElse(fail("should fail")).getMessage

  private val walletNotFoundError = "iog.psg.cardano.jpi.CardanoApiException: Message: Wallet not found, Code: 404"

  "GET /wallets" should "return wallets list" in {
    val response = api.listWallets().toCompletableFuture.get()
    response.size() shouldBe 1
    response.get(0) shouldBe wallet
  }

  "GET /wallets/{walletId}" should "return existing wallet" in {
    val response = api.getWallet(wallet.id).toCompletableFuture.get()
    response shouldBe wallet
  }

  it should "return 404 if wallet does not exists" in {
    tryGetErrorMessage(api.getWallet("invalid_wallet_id")) shouldBe walletNotFoundError
  }

  "GET /network/information" should "return network information" in {
    api.networkInfo.toCompletableFuture.get() shouldBe networkInfo
  }

  "POST /wallets" should "create a wallet, using all params" in {
    api
      .createRestore(
        randomWalletName,
        walletPassphrase,
        mnemonicSentence.mnemonicSentence.toList.asJava,
        mnemonicSecondFactor.mnemonicSentence.toList.asJava,
        addressPoolGap
      )
      .toCompletableFuture
      .get() shouldBe wallet.copy(name = randomWalletName, addressPoolGap = addressPoolGap)
  }

  it should "create a wallet, without using mnemonicSecondFactor param" in new CustomInMemoryFixture {
    override val postWalletFieldsToCheck: List[String] = List("name", "passphrase", "mnemonic_sentence", "address_pool_gap")

    customApi
      .createRestore(
        randomWalletName,
        walletPassphrase,
        mnemonicSentence.mnemonicSentence.toList.asJava,
        addressPoolGap
      )
      .toCompletableFuture
      .get() shouldBe wallet.copy(name = randomWalletName, addressPoolGap = addressPoolGap)
  }

  "GET /wallets/{walletId}/addresses?state=unused" should "return wallet's unused addresses" in {
    val ids = api.listAddresses(wallet.id, AddressFilter.UNUSED).toCompletableFuture.get().asScala.toList.map(_.id)
    ids shouldBe unUsedAddresses.map(_.id)
  }

  it should "return wallet's used addresses" in {
    val ids = api.listAddresses(wallet.id, AddressFilter.USED).toCompletableFuture.get().asScala.toList.map(_.id)
    ids shouldBe usedAddresses.map(_.id)
  }

  it should "return wallet's used + unused addresses" in {
    val ids = api.listAddresses(wallet.id).toCompletableFuture.get().asScala.toList.map(_.id)
    ids shouldBe addresses.map(_.id)
  }

  it should "return wallet not found error" in {
    tryGetErrorMessage(api.listAddresses("invalid_wallet_id", AddressFilter.USED)) shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/transactions" should "return wallet's transactions" in {
    val builder = ListTransactionsParamBuilder.create(wallet.id)
    val transactions = api.listTransactions(builder).toCompletableFuture.get().asScala

    transactions.map(_.id) shouldBe transactionsIdsDesc
  }

  it should "return not found error" in {
    val builder = ListTransactionsParamBuilder.create("invalid_wallet_id")
    tryGetErrorMessage(api.listTransactions(builder)) shouldBe walletNotFoundError
  }

  it should "run request with proper params" in {
    val builder = ListTransactionsParamBuilder
      .create(wallet.id)
      .withStartTime(ZonedDateTime.parse("2000-01-01T00:00:00.000Z"))
      .withEndTime(ZonedDateTime.parse("2001-01-01T00:00:00.000Z"))
      .withOrder(iog.psg.cardano.jpi.Order.ASCENDING)
      .withMinwithdrawal(100)

    val transactions = api.listTransactions(builder).toCompletableFuture.get().asScala
    transactions.map(_.id) shouldBe oldTransactionsIdsAsc
  }

  "GET /wallets/{walletId}/transactions/{transactionId}" should "return transaction" in {
    api
      .getTransaction(wallet.id, firstTransactionId)
      .toCompletableFuture
      .get()
      .id shouldBe firstTransactionId
  }

  it should "return not found error" in {
    tryGetErrorMessage(
      api.getTransaction(wallet.id, "not_existing_id")
    ) shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Transaction not found, Code: 404"
  }

  "POST /wallets/{walletId}/transactions" should "create transaction" in {
    api
      .createTransaction(wallet.id, walletPassphrase, payments.payments.asJava, txMetadata, withdrawal)
      .toCompletableFuture
      .get()
      .id shouldBe firstTransactionId
  }

  it should "create transaction without metadata and with default withdrawal" in new CustomInMemoryFixture {
    override val postTransactionFieldsToCheck: List[String] = List("passphrase", "payments", "withdrawal")
    override val withdrawal: String = "self"

    customApi
      .createTransaction(wallet.id, walletPassphrase, payments.payments.asJava)
      .toCompletableFuture
      .get()
      .id shouldBe firstTransactionId
  }

  it should "return not found" in {
    tryGetErrorMessage(
      api.createTransaction("invalid_wallet_id", "MySecret", payments.payments.asJava)
    ) shouldBe walletNotFoundError
  }

  "POST /wallets/{fromWalletId}/payment-fees" should "estimate fee" in {
    api.estimateFee(wallet.id, payments.payments.asJava, withdrawal, txMetadata).toCompletableFuture.get() shouldBe estimateFeeResponse
  }

  it should "estimate fee without metadata and with default withdrawal" in new CustomInMemoryFixture {
    override val postEstimateFeeFieldsToCheck: List[String] = List("payments", "withdrawal")
    override val withdrawal: String = "self"

    customApi.estimateFee(wallet.id, payments.payments.asJava).toCompletableFuture.get() shouldBe estimateFeeResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.estimateFee("invalid_wallet_id", payments.payments.asJava)) shouldBe walletNotFoundError
  }

  "POST /wallets/{walletId}/coin-selections/random" should "fund payments" in {
    api.fundPayments(wallet.id, payments.payments.asJava).toCompletableFuture.get() shouldBe fundPaymentsResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.fundPayments("invalid_wallet_id", payments.payments.asJava)) shouldBe walletNotFoundError
  }

  "PUT /wallets/{walletId/passphrase" should "update passphrase" in {
    api.updatePassphrase(wallet.id, oldPassword, newPassword).toCompletableFuture.get() shouldBe null
  }

  it should "return not found" in {
    tryGetErrorMessage(
      api.updatePassphrase("invalid_wallet_id", oldPassword, newPassword)
    ) shouldBe walletNotFoundError
  }

  "PUT /wallets/{walletId" should "update name" in {
    val newName = s"${wallet.name}_updated"
    api.updateName(wallet.id, newName).toCompletableFuture.get().name shouldBe newName
  }

  it should "return not found" in {
    tryGetErrorMessage(api
      .updateName("invalid_wallet_id", "random_name")) shouldBe walletNotFoundError
  }

  "DELETE /wallets/{walletId" should "delete wallet" in {
    api.deleteWallet(wallet.id).toCompletableFuture.get() shouldBe null
  }

  it should "return not found" in {
    tryGetErrorMessage(api.deleteWallet("invalid_wallet_id")) shouldBe walletNotFoundError
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-jpi-test-system")

  private def getCurrentSpecAS: ActorSystem = as

  sealed trait CustomInMemoryFixture extends Configure
    with ModelCompare
    with ScalaFutures
    with InMemoryCardanoApi
    with DummyModel
    with JsonFiles {
    override implicit val as: ActorSystem = getCurrentSpecAS
    lazy val customApi: jpi.CardanoApi = JpiResponseCheck.buildWithPredefinedApiExecutor(inMemoryExecutor, as)
  }

}
