package iog.psg.cardano

import java.nio.file.Paths
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
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
    with ResourceFiles
    with DummyModel
    with CustomPatienceConfiguration {

  lazy val api = CardanoApi(baseUrl)

  private val addressNotFoundError = ErrorMessage(s"Addresses not found", "404")
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

  "GET /network/clock" should "return network clock" in {
    api.networkClock(forceNtpCheck = Some(true)).executeOrFail() shouldBe networkClockForced
  }

  it should "return network clock without forced ntp check" in {
    api.networkClock(Some(false)).executeOrFail() shouldBe networkClock
  }

  it should "return network clock without forced ntp check param" in {
    api.networkClock().executeOrFail() shouldBe networkClock
  }

  "GET /network/parameters" should "return network parameters" in {
    api.networkParameters().executeOrFail() shouldBe networkParameters
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
    api.listAddresses(wallet.id, None).executeOrFail().map(_.id) shouldBe addressesIds.map(_.id)
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

  "DELETE /wallets/{walletId}/transactions" should "forget pending transaction" in {
    api.deleteTransaction(wallet.id, firstTransactionId).executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api.deleteTransaction("invalid_wallet_id", firstTransactionId).executeExpectingErrorOrFail() shouldBe walletNotFoundError
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

  "PUT /wallets/{walletId}" should "update name" in {
    val newName = s"${wallet.name}_updated"
    api.updateName(wallet.id, newName).executeOrFail().name shouldBe newName
  }

  it should "return not found" in {
    api
      .updateName("invalid_wallet_id", "random_name")
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "PUT /wallets/{walletId/passphrase" should "update passphrase" in {
    api.updatePassphrase(wallet.id, oldPassword, newPassword).executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api
      .updatePassphrase("invalid_wallet_id", oldPassword, newPassword)
      .executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "DELETE /wallets/{walletId}" should "delete wallet" in {
    api.deleteWallet(wallet.id).executeOrFail() shouldBe ()
  }

  it should "return not found" in {
    api.deleteWallet("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /addresses/{addressId}" should "inspect address" in {
    api.inspectAddress(addressToInspect.id).executeOrFail() shouldBe address
  }

  it should "return not found" in {
    api.inspectAddress("invalid_address_id").executeExpectingErrorOrFail() shouldBe addressNotFoundError
  }

  "GET /wallets/{walletId}/statistics/utxos" should "get UTxOs statistics" in {
    api.getUTxOsStatistics(wallet.id).executeOrFail() shouldBe uTxOStatistics
  }

  it should "return not found" in {
    api.getUTxOsStatistics("invalid_address_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "POST /proxy/transactions" should "submit a transaction that was created and signed outside of cardano-wallet" in {
    api.postExternalTransaction(txRawContent).executeOrFail() shouldBe jsonFileProxyTransactionResponse
  }

  it should "fail on invalid request body" in {
    api.postExternalTransaction("1234567890").executeExpectingErrorOrFail() shouldBe ErrorMessage("Invalid binary string", "400")
  }

  "POST /wallets/{walletId}/migrations" should "submit one or more transactions which transfers all funds from a Shelley wallet to a set of addresses" in {
    api.migrateShelleyWallet(wallet.id, walletPassphrase, unUsedAddresses.map(_.id)).executeOrFail() shouldBe jsonFileMigrationsResponse
  }

  it should "return not found" in {
    api.migrateShelleyWallet("invalid_address_id", walletPassphrase, unUsedAddresses.map(_.id)).executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /wallets/{walletId}/migrations" should "calculate the exact cost of sending all funds from particular Shelley wallet to a set of addresses" in {
    api.getShelleyWalletMigrationInfo(wallet.id).executeOrFail() shouldBe jsonFileMigrationCostsResponse
  }

  it should "return not found" in {
    api.getShelleyWalletMigrationInfo("invalid_address_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "GET /stake-pools" should "List all known stake pools ordered by descending non_myopic_member_rewards." in {
    api.listStakePools(stake = 12345).executeOrFail() shouldBe jsonFileStakePoolsResponse
  }

  it should "return error" in {
    api.listStakePools(stake = -1).executeExpectingErrorOrFail() shouldBe ErrorMessage("Invalid stake parameter", "400")
  }

  "GET /wallets/{walletId}/delegation-fees" should "Estimate fee for joining or leaving a stake pool" in {
    api.estimateFeeStakePool(wallet.id).executeOrFail() shouldBe estimateFeeResponse
  }

  it should "return not found" in {
    api.estimateFeeStakePool("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe walletNotFoundError
  }

  "PUT /stake-pools/{stakePoolId}/wallets/{walletId}" should "Delegate all (current and future) addresses from the given wallet to the given stake pool" in {
    api.joinStakePool(wallet.id, stakePoolId, walletPassphrase).executeOrFail() shouldBe jsonFileMigrationResponse
  }

  it should "return not found" in {
    api.joinStakePool("invalid_wallet_id", stakePoolId, walletPassphrase).executeExpectingErrorOrFail() shouldBe ErrorMessage("Not found", "404")
  }

  "DELETE /stake-pools/*/wallets/{walletId}" should "Stop delegating completely" in {
    api.quitStakePool(wallet.id, walletPassphrase).executeOrFail() shouldBe jsonFileMigrationResponse
  }

  it should "return not found" in {
    api.quitStakePool("invalid_wallet_id", walletPassphrase).executeExpectingErrorOrFail() shouldBe ErrorMessage("Not found", "404")
  }

  "GET /stake-pools/maintenance-actions" should "return current status of the stake pools maintenance actions" in {
    api.getMaintenanceActions().executeOrFail() shouldBe jsonFileStakePoolsMaintenanceActions
  }

  "POST /stake-pools/maintenance-actions" should "perform maintenance actions on stake pools" in {
    api.postMaintenanceAction().executeOrFail() shouldBe ()
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

}
