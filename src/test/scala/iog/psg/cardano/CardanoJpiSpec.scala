package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.jpi.JpiResponseCheck
import iog.psg.cardano.util.{Configure, DummyModel, InMemoryCardanoApi, ModelCompare}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Try

class CardanoJpiSpec extends AnyFlatSpec with Matchers with Configure with ModelCompare with InMemoryCardanoApi with DummyModel {

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
    response.swap.getOrElse(fail("Should fail")).getMessage shouldBe "iog.psg.cardano.jpi.CardanoApiException: Message: Wallet not found, Code: 404"
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-jpi-test-system")
}
