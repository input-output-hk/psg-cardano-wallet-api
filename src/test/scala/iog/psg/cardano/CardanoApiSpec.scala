package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.ErrorMessage
import iog.psg.cardano.CardanoApiCodec.NetworkInfo
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

  override implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

}
