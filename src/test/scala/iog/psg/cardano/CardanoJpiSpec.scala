package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.jpi.{ AddressFilter, JpiResponseCheck, ListTransactionsParamBuilder }
import iog.psg.cardano.util.{ Configure, DummyModel, InMemoryCardanoApi, ModelCompare }
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
    with DummyModel {

  lazy val api = JpiResponseCheck.buildWithPredefinedApiExecutor(inMemoryExecutor, as)

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
    val response = Try(api.getWallet("invalid_wallet_id").toCompletableFuture.get()).toEither
    response.swap
      .getOrElse(fail("Should fail"))
      .getMessage shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Wallet not found, Code: 404"
  }

  "GET /network/information" should "return network information" in {
    api.networkInfo.toCompletableFuture.get() shouldBe networkInfo
  }

  "POST /wallets" should "" in {
    api
      .createRestore(wallet.name, "Pass9128!", mnemonicSentence.mnemonicSentence.toList.asJava, 5)
      .toCompletableFuture
      .get() shouldBe wallet
  }

  "GET /wallets/{walletId}/addresses?state=unused" should "return wallet's unused addresses" in {
    val ids = api.listAddresses(wallet.id, AddressFilter.UNUSED).toCompletableFuture.get().asScala.toList.map(_.id)
    ids shouldBe unUsedAddresses.map(_.id)
  }

  it should "return wallet'snused addresses" in {
    val ids = api.listAddresses(wallet.id, AddressFilter.USED).toCompletableFuture.get().asScala.toList.map(_.id)
    ids shouldBe usedAddresses.map(_.id)
  }

  "GET /wallets/{walletId}/transactions" should "return wallet's transactions" in {
    val builder = ListTransactionsParamBuilder.create(wallet.id)
    api.listTransactions(builder).toCompletableFuture.get().asScala.map(_.id) shouldBe Seq(
      createdTransactionResponse.id
    )
  }

  "POST /wallets/{walletId}/transactions" should "create transaction" in {
    api
      .createTransaction(wallet.id, "MySecret", payments.payments.asJava)
      .toCompletableFuture
      .get()
      .id shouldBe createdTransactionResponse.id
  }

  "POST /wallets/{fromWalletId}/payment-fees" should "estimate fee" in {
    api.estimateFee(wallet.id, payments.payments.asJava).toCompletableFuture.get() shouldBe estimateFeeResponse
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-jpi-test-system")
}
