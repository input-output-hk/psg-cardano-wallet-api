package iog.psg.cardano

import java.time.ZonedDateTime
import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.jpi.{AddressFilter, JpiResponseCheck, ListTransactionsParamBuilder}
import iog.psg.cardano.util._
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
    with ResourceFiles {

  lazy val api = JpiResponseCheck.buildWithPredefinedApiExecutor(inMemoryExecutor, as)

  private def tryGetErrorMessage[T](completionStage: CompletionStage[T]) =
    Try(completionStage.toCompletableFuture.get()).toEither.swap.getOrElse(fail("should fail")).getMessage

  private val addressNotFoundError = "iog.psg.cardano.jpi.CardanoApiException: Message: Addresses not found, Code: 404"
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

  "GET /network/clock" should "return network clock with forced ntp check" in {
    api.networkClock(true).toCompletableFuture.get() shouldBe networkClockForced
  }

  it should "return network clock without forced ntp check" in {
    api.networkClock(false).toCompletableFuture.get() shouldBe networkClock
  }

  it should "return network clock without forced ntp check param" in {
    api.networkClock().toCompletableFuture.get() shouldBe networkClock
  }

  "GET /network/parameters" should "return network clock with forced ntp check" in {
    api.networkParameters().toCompletableFuture.get() shouldBe networkParameters
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
    ids shouldBe addressesIds.map(_.id)
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

  "DELETE /wallets/{walletId}/transactions" should "forget pending transaction" in {
    api.deleteTransaction(wallet.id, firstTransactionId).toCompletableFuture.get() shouldBe null
  }

  it should "return not found" in {
    tryGetErrorMessage(api.deleteTransaction("invalid_wallet_id", firstTransactionId)) shouldBe walletNotFoundError
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

  "GET /addresses/{addressId}" should "inspect address" in {
    api.inspectAddress(addressToInspect.id).toCompletableFuture.get() shouldBe address
  }

  it should "return not found" in {
    tryGetErrorMessage(api.inspectAddress("invalid_address_id")) shouldBe addressNotFoundError
  }

  "GET /wallets/{walletId}/statistics/utxos" should "get UTxOs statistics" in {
    api.getUTxOsStatistics(wallet.id).toCompletableFuture.get() shouldBe uTxOStatistics
  }

  it should "return not found" in {
    tryGetErrorMessage(api.getUTxOsStatistics("invalid_address_id")) shouldBe walletNotFoundError
  }

  "POST /proxy/transactions" should "submit a transaction that was created and signed outside of cardano-wallet" in {
    api.postExternalTransaction(txRawContent).toCompletableFuture.get() shouldBe jsonFileProxyTransactionResponse
  }

  it should "fail on invalid request body" in {
    tryGetErrorMessage(api.postExternalTransaction("1234567890")) shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Invalid binary string, Code: 400"
  }

  "POST /wallets/{walletId}/migrations" should "submit one or more transactions which transfers all funds from a Shelley wallet to a set of addresses" in {
    api.migrateShelleyWallet(wallet.id, walletPassphrase, unUsedAddresses.map(_.id).asJava).toCompletableFuture.get().asScala shouldBe jsonFileMigrationsResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.migrateShelleyWallet("invalid_address_id", walletPassphrase, unUsedAddresses.map(_.id).asJava)) shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/migrations" should "calculate the exact cost of sending all funds from particular Shelley wallet to a set of addresses" in {
    api.getShelleyWalletMigrationInfo(wallet.id).toCompletableFuture.get() shouldBe jsonFileMigrationCostsResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.getShelleyWalletMigrationInfo("invalid_address_id")) shouldBe walletNotFoundError
  }

  "GET /stake-pools" should "List all known stake pools ordered by descending non_myopic_member_rewards." in {
    api.listStakePools(12345).toCompletableFuture.get().asScala shouldBe jsonFileStakePoolsResponse
  }

  it should "return error" in {
    tryGetErrorMessage(api.listStakePools(-1)) shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Invalid stake parameter, Code: 400"
  }

  "GET /wallets/{walletId}/delegation-fees" should "Estimate fee for joining or leaving a stake pool" in {
    api.estimateFeeStakePool(wallet.id).toCompletableFuture.get() shouldBe estimateFeeResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.estimateFeeStakePool("invalid_wallet_id")) shouldBe walletNotFoundError
  }

  "PUT /stake-pools/{stakePoolId}/wallets/{walletId}" should "Delegate all (current and future) addresses from the given wallet to the given stake pool" in {
    api.joinStakePool(wallet.id, stakePoolId, walletPassphrase).toCompletableFuture.get() shouldBe jsonFileMigrationResponse
  }

  it should "return not found" in {
   tryGetErrorMessage(api.joinStakePool("invalid_wallet_id", stakePoolId, walletPassphrase)) shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Not found, Code: 404"
  }

  "DELETE /stake-pools/*/wallets/{walletId}" should "Stop delegating completely" in {
    api.quitStakePool(wallet.id, walletPassphrase).toCompletableFuture.get() shouldBe jsonFileMigrationResponse
  }

  it should "return not found" in {
    tryGetErrorMessage(api.quitStakePool("invalid_wallet_id", walletPassphrase)) shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Not found, Code: 404"
  }

  "GET /stake-pools/maintenance-actions" should "return current status of the stake pools maintenance actions" in {
    api.getMaintenanceActions().toCompletableFuture.get() shouldBe jsonFileStakePoolsMaintenanceActions
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-jpi-test-system")

  private def getCurrentSpecAS: ActorSystem = as

  sealed trait CustomInMemoryFixture extends Configure
    with ModelCompare
    with ScalaFutures
    with InMemoryCardanoApi
    with DummyModel
    with ResourceFiles {
    override implicit val as: ActorSystem = getCurrentSpecAS
    lazy val customApi: jpi.CardanoApi = JpiResponseCheck.buildWithPredefinedApiExecutor(inMemoryExecutor, as)
  }

}
