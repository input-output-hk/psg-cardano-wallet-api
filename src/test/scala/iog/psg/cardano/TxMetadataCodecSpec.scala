package iog.psg.cardano

import akka.util.ByteString
import io.circe.syntax.EncoderOps
import io.circe.{ParsingFailure, parser}
import iog.psg.cardano.CardanoApiCodec.ImplicitCodecs._
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
      MetadataValueLong(14), MetadataValueLong(42), MetadataValueStr("1337"), MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f"))
    )))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"3":{"list":[{"int":14},{"int":42},{"string":"1337"},{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"}]}}"""
  }

  it should "encode map value to proper json" in {
    val map: Map[Long, MetadataValue] = Map(4L -> MetadataValueMap(Map(
      MetadataValueStr("key") -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
      MetadataValueLong(14) -> MetadataValueLong(42)
    )))
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces
    metaInJsonStr shouldBe """{"4":{"map":[{"k":{"string":"key"},"v":{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"}},{"k":{"int":14},"v":{"int":42}}]}}"""
  }

  it should "encode properly all types to json" in {
    val map: Map[Long, MetadataValue] = Map(
      0L -> MetadataValueStr("cardano"),
      1L -> MetadataValueLong(14),
      2L -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
      3L -> MetadataValueArray(List(
        MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")), MetadataValueLong(42), MetadataValueStr("1337")
      )),
      4L -> MetadataValueMap(Map(
        MetadataValueStr("key") -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
        MetadataValueLong(14) -> MetadataValueLong(42)
      ))
    )
    val metaDataIn: TxMetadataIn = TxMetadataMapIn(map)
    val metaInJsonStr = metaDataIn.asJson.noSpaces

    metaInJsonStr shouldBe """{"0":{"string":"cardano"},"1":{"int":14},"2":{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"},"3":{"list":[{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"},{"int":42},{"string":"1337"}]},"4":{"map":[{"k":{"string":"key"},"v":{"bytes":"2512a00e9653fe49a44a5886202e24d77eeb998f"}},{"k":{"int":14},"v":{"int":42}}]}}""".stripMargin
  }

  "txMetadataOut toMapMetadataStr" should "be parsed properly" in {
    txMetadataOut.toMetadataMap.getOrElse(fail("could not parse map")) shouldBe Map(
      0 -> MetadataValueStr("cardano"),
      1 -> MetadataValueLong(14),
      2 -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
      3 -> MetadataValueArray(Seq(
        MetadataValueLong(14),
        MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
        MetadataValueStr("1337"),
        MetadataValueMap(Map(MetadataValueLong(1) -> MetadataValueLong(3)))
      )),
      4 -> MetadataValueMap(
        Map(
          MetadataValueStr("key") -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")),
          MetadataValueLong(14) -> MetadataValueLong(42)
      )))
  }

  it should "fail on missing type field" in {
    val jsonWithInvalidTypeField = parser.parse("""{"0":"cardano"}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Missing value under key: DownField(0)"
  }

  it should "fail on unsupported type field" in {
    val jsonWithInvalidTypeField = parser.parse("""{"0":{"superdouble":"cardano"}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Invalid type 'superdouble': DownField(0)"
  }

  it should "fail on unsupported type field in list" in {
    val jsonWithInvalidTypeField = parser.parse("""{"3":{"list":[{"superdouble":14},{"int":42},{"string":"1337"}]}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Invalid type 'superdouble': DownField(3)"
  }

  it should "fail on missing type field in list" in {
    val jsonWithInvalidTypeField = parser.parse("""{"3":{"list":[14,{"int":42},{"string":"1337"}]}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Missing value under key: DownField(3)"
  }

  it should "fail on unsupported type field in map" in {
    val jsonWithInvalidTypeField = parser.parse("""{"4":{"map":[{"k":{"string":"key"},"v":{"superdouble":"value"}},{"k":{"int":14},"v":{"int":42}}]}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Invalid type 'superdouble': DownField(4)"
  }

  it should "fail on missing 'k' field in map" in {
    val jsonWithInvalidTypeField = parser.parse("""{"4":{"map":[{"kx":{"string":"key"},"v":{"superdouble":"value"}},{"k":{"int":14},"v":{"int":42}}]}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Missing 'k' value: DownField(4)"
  }

  it should "fail on int defined as a string type" in {
    val jsonWithInvalidTypeField = parser.parse("""{"0":{"string": 12345}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Not a String type: DownField(string),DownField(0)"
  }

  it should "fail on string defined as a int type" in {
    val jsonWithInvalidTypeField = parser.parse("""{"0":{"int":"abc123"}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Not a Long type: DownField(int),DownField(0)"
  }

  it should "fail on int defined as a bytes type" in {
    val jsonWithInvalidTypeField = parser.parse("""{"0":{"bytes":12345}}""").getOrElse(fail("Invalid json structure"))
    val tvMeta = TxMetadataOut(jsonWithInvalidTypeField)

    val error = tvMeta.toMetadataMap.swap.getOrElse(fail("Should fail"))
    error.getMessage() shouldBe "Not a Bytes type: DownField(bytes),DownField(0)"
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