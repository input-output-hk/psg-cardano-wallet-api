package iog.psg.cardano

import java.time.ZonedDateTime

import akka.util.ByteString
import io.circe.syntax.EncoderOps
import iog.psg.cardano.codecs.CardanoApiCodec._
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
        nextEpoch = NextEpoch(ZonedDateTime.parse("2000-01-02T03:04:05.000Z"), 14)
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

  "TxMetadataMapIn encode" should "encode string value to proper json" in  {
    val map: Map[Long, MetadataValue] = Map(0L -> MetadataValueStr("cardano"))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"0":{"string":"cardano"}}"""
  }

  it should "encode long value to proper json" in {
    val map: Map[Long, MetadataValue] = Map(1L -> MetadataValueLong(14))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"1":{"int":14}}"""
  }

  it should "encode byte string value to proper json" in {
    val map: Map[Long, MetadataValue] = Map(2L -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"2":{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"}}"""
  }

  it should "encode list value to proper json" in {
    val map: Map[Long, MetadataValue] = Map(3L -> MetadataValueArray(List(
      MetadataValueLong(14), MetadataValueLong(42), MetadataValueStr("1337")
    )))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"3":{"list":[{"int":14},{"int":42},{"string":"1337"}]}}"""
  }

  it should "encode map value to proper json" in {
    val map: Map[Long, MetadataValue] = Map(4L -> MetadataValueMap(Map(
      MetadataValueStr("key") -> MetadataValueStr("value"),
      MetadataValueLong(14) -> MetadataValueLong(42)
    )))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces
    metaInJsonStr shouldBe """{"4":{"map":[{"k":{"string":"key"},"v":{"string":"value"}},{"k":{"int":14},"v":{"int":42}}]}}"""
  }

  it should "encode properly all types to json" in {
    val map: Map[Long, MetadataValue] = Map(
      0L -> MetadataValueStr("cardano"),
      1L -> MetadataValueLong(14),
      2L -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
      3L -> MetadataValueArray(List(
        MetadataValueLong(14), MetadataValueLong(42), MetadataValueStr("1337")
      )),
      4L -> MetadataValueMap(Map(
        MetadataValueStr("key") -> MetadataValueStr("value"),
        MetadataValueLong(14) -> MetadataValueLong(42)
      ))
    )
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"0":{"string":"cardano"},"1":{"int":14},"2":{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"},"3":{"list":[{"int":14},{"int":42},{"string":"1337"}]},"4":{"map":[{"k":{"string":"key"},"v":{"string":"value"}},{"k":{"int":14},"v":{"int":42}}]}}""".stripMargin
  }

  "txMetadataOut toMapMetadataStr" should "be pared properly" in {
    txMetadataOut.toMapMetadataStr.getOrElse(fail("could not parse map")) shouldBe Map(
      0 -> MetadataValueStr("cardano"),
      1 -> MetadataValueLong(14),
      2 -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
      3 -> MetadataValueArray(Seq(
        MetadataValueLong(14),
        MetadataValueLong(42),
        MetadataValueStr("1337")
      )),
      4 -> MetadataValueMap(
        Map(
          MetadataValueStr("key") -> MetadataValueStr("value"),
          MetadataValueLong(14) -> MetadataValueLong(42)
      )))
  }

}