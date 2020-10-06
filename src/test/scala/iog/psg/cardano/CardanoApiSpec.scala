package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.util.{DummyModel, InMemoryCardanoApi, ModelCompare}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiSpec extends AnyFlatSpec with Matchers with ModelCompare with ScalaFutures with InMemoryCardanoApi with DummyModel {

  lazy val api = new CardanoApi(baseUrl)

  "GET /wallets" should "return wallets list" in {
    inMemoryExecutor.execute(api.listWallets).futureValue.getOrElse(fail("Api request failed")).head shouldBe wallet
  }

  "GET /wallets/{walletId}" should "return existing wallet" in {
    inMemoryExecutor.execute(api.getWallet(wallet.id)).futureValue.getOrElse(fail("Api request failed")) shouldBe wallet
  }

  it should "return 404 if wallet does not exists" in {
    inMemoryExecutor.execute(api.getWallet("invalid_wallet_id")).futureValue.swap.getOrElse(fail("Should return an error.")) shouldBe ErrorMessage(s"Wallet not found", "404")
  }

  override implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

}
