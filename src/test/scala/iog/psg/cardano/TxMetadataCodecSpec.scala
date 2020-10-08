package iog.psg.cardano

import akka.util.ByteString
import io.circe.ParsingFailure
import io.circe.syntax.EncoderOps
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.DummyModel
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TxMetadataCodecSpec extends AnyFlatSpec with Matchers with DummyModel {

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

  "txMetadataOut toMapMetadataStr" should "be parsed properly" in {
    txMetadataOut.toMetadataMap.getOrElse(fail("could not parse map")) shouldBe Map(
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

  "Raw Good TxMetadata" should "be parsed properly" in {
    val asString = txMetadataOut.json.noSpaces
    val Right(rawTxMetaJsonIn) = JsonMetadata.parse(asString)
    val rawTxMetaJsonIn2 = JsonMetadata(asString)
    rawTxMetaJsonIn.metadataCompliantJson shouldBe txMetadataOut.json
    rawTxMetaJsonIn2.metadataCompliantJson shouldBe txMetadataOut.json
  }

  "Raw Bad TxMetadata" should "be rejected" in {
    val asString = txMetadataOut.json.noSpaces
    val badJson = asString.substring(0, asString.length - 1)
    val Left(ParsingFailure(_, _)) = JsonMetadata.parse(badJson)
    intercept[Exception](JsonMetadata(badJson))
  }
}