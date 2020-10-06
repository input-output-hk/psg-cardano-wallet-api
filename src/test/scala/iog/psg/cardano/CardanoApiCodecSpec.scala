package iog.psg.cardano

import java.time.ZonedDateTime

import io.circe.Decoder
import io.circe.parser._
import io.circe.generic.auto._
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.{DummyModel, ModelCompare}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class CardanoApiCodecSpec extends AnyFlatSpec with Matchers with ModelCompare with DummyModel {

  "Wallet" should "be decoded properly" in {
    val decoded = decodeJsonFile[Wallet]("wallet.json")

    compareWallets(decoded, wallet)
  }

  it should "decode wallet's list" in {
    val decodedWallets = decodeJsonFile[Seq[Wallet]]("wallets.json")

    decodedWallets.size shouldBe 1
    compareWallets(decodedWallets.head, wallet)
  }

  "network information" should "be decoded properly" in {
    val decoded = decodeJsonFile[NetworkInfo]("netinfo.json")

    compareNetworkInformation(
      decoded,
      NetworkInfo(
        syncProgress = SyncStatus(SyncState.ready, None),
        networkTip = networkTip.copy(height = None),
        nodeTip = nodeTip,
        nextEpoch = NextEpoch(ZonedDateTime.parse("2019-02-27T14:46:45.000Z"), 14)
      )
    )

  }

  "list addresses" should "be decoded properly" in {
    val decoded = decodeJsonFile[Seq[WalletAddressId]]("addresses.json")
    decoded.size shouldBe 1

    compareAddress(decoded.head, WalletAddressId(id = addressIdStr, Some(AddressFilter.used)))
  }

  "list transactions" should "be decoded properly" in {
    val decoded = decodeJsonFile[Seq[CreateTransactionResponse]]("transactions.json")
    decoded.size shouldBe 1

    compareTransaction(decoded.head, createdTransactionResponse)
  }

  it should "decode one transaction" in {
    val decoded = decodeJsonFile[CreateTransactionResponse]("transaction.json")

    compareTransaction(decoded, createdTransactionResponse)
  }

  "estimate fees" should "be decoded properly" in {
    val decoded = decodeJsonFile[EstimateFeeResponse]("estimate_fees.json")

    compareEstimateFeeResponse(decoded, estimateFeeResponse)
  }

  "fund payments" should "be decoded properly" in {
    val decoded = decodeJsonFile[FundPaymentsResponse]("coin_selections_random.json")

    compareFundPaymentsResponse(decoded, fundPaymentsResponse)
  }

  private def getJsonFromFile(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    source.close()
    jsonStr
  }


  private def decodeJsonFile[T](file: String)(implicit dec: Decoder[T]) = {
    val jsonStr = getJsonFromFile(file)
    decode[T](jsonStr).getOrElse(fail("Could not decode wallet"))
  }

}