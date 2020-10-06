package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, EstimateFeeResponse, NetworkInfo}
import iog.psg.cardano.util.{DummyModel, InMemoryCardanoApi, ModelCompare}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiSpec
    extends AnyFlatSpec
    with Matchers
    with ModelCompare
    with ScalaFutures
    with InMemoryCardanoApi
    with DummyModel {

  lazy val api = new CardanoApi(baseUrl)

  "GET /wallets" should "return wallets list" in {
    api.listWallets.executeOrFail().head shouldBe wallet
  }

  "GET /wallets/{walletId}" should "return existing wallet" in {
    api.getWallet(wallet.id).executeOrFail() shouldBe wallet
  }

  it should "return 404 if wallet does not exists" in {
    api.getWallet("invalid_wallet_id").executeExpectingErrorOrFail() shouldBe ErrorMessage(s"Wallet not found", "404")
  }

  "GET /network/information" should "return network information" in {
    api.networkInfo.executeOrFail() shouldBe networkInfo
  }

  "POST /wallets" should "" in {
    api.createRestoreWallet(wallet.name, "Pass9128!", mnemonicSentence).executeOrFail() shouldBe wallet
  }

  "GET /wallets/{walletId}/addresses?state=unused" should "return wallet's unused addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.unUsed)).executeOrFail().map(_.id) shouldBe unUsedAddresses.map(_.id)
  }

  it should "return wallet'snused addresses" in {
    api.listAddresses(wallet.id, Some(AddressFilter.used)).executeOrFail().map(_.id) shouldBe usedAddresses.map(_.id)
  }

  "GET /wallets/{walletId}/transactions" should "return wallet's transactions" in {
    api.listTransactions(wallet.id).executeOrFail().map(_.id) shouldBe Seq(createdTransactionResponse.id)
  }

  "POST /wallets/{walletId}/transactions" should "create transaction" in {
    api.createTransaction(
      fromWalletId = wallet.id, passphrase = "MySecret", payments = payments, metadata = None, withdrawal = None
    ).executeOrFail().id shouldBe createdTransactionResponse.id
  }

  "POST /wallets/{fromWalletId}/payment-fees" should "estimate fee" in {
    api.estimateFee(wallet.id, payments).executeOrFail() shouldBe estimateFeeResponse
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

}
