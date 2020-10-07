package iog.psg.cardano

import java.time.ZonedDateTime

import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.{DummyModel, JsonFiles, ModelCompare}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiCodecSpec extends AnyFlatSpec with Matchers with ModelCompare with DummyModel with JsonFiles {

  "Wallet" should "be decoded properly" in {
    compareWallets(jsonFileWallet, wallet)
  }

  it should "decode wallet's list" in {
    jsonFileWallets.size shouldBe 1
    compareWallets(jsonFileWallets.head, wallet)
  }

  "network information" should "be decoded properly" in {
    compareNetworkInformation(
      jsonFileNetInfo,
      NetworkInfo(
        syncProgress = SyncStatus(SyncState.ready, None),
        networkTip = networkTip.copy(height = None),
        nodeTip = nodeTip,
        nextEpoch = NextEpoch(ZonedDateTime.parse("2019-02-27T14:46:45.000Z"), 14)
      )
    )
  }

  "list addresses" should "be decoded properly" in {
    jsonFileAddresses.size shouldBe 3

    compareAddress(jsonFileAddresses.head, WalletAddressId(id = addressIdStr, Some(AddressFilter.unUsed)))
  }

  "list transactions" should "be decoded properly" in {
    jsonFileCreatedTransactionsResponse.size shouldBe 1
    compareTransaction(jsonFileCreatedTransactionsResponse.head, createdTransactionResponse)
  }

  it should "decode one transaction" in {
    compareTransaction(jsonFileCreatedTransactionResponse, createdTransactionResponse)
  }

  "estimate fees" should "be decoded properly" in {
    compareEstimateFeeResponse(jsonFileEstimateFees, estimateFeeResponse)
  }

  "fund payments" should "be decoded properly" in {
    compareFundPaymentsResponse(jsonFileCoinSelectionRandom, fundPaymentsResponse)
  }

}