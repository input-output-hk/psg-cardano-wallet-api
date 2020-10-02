package iog.psg.cardano

import java.time.ZonedDateTime

import akka.util.ByteString
import io.circe.Decoder
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
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
      networkInfo.copy(nextEpoch = networkInfo.nextEpoch.copy(epochStartTime = ZonedDateTime.parse("2019-02-27T14:46:45Z")))
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
    val test =
      TxMetadataOut(parse("""{
                            |  "0": {
                            |    "string": "cardano"
                            |  },
                            |  "1": {
                            |    "int": 14
                            |  },
                            |  "2": {
                            |    "bytes": "2512a00e9653fe49a44a5886202e24d77eeb998f"
                            |  },
                            |  "3": {
                            |    "list": [
                            |      {
                            |        "int": 14
                            |      },
                            |      {
                            |        "int": 42
                            |      },
                            |      {
                            |        "string": "1337"
                            |      }
                            |    ]
                            |  }
                            |}""".stripMargin).getOrElse(fail("Invalid json.")))

    println("test.toMapMetadataStr: "+test.toMapMetadataStr)
    test.toMapMetadataStr.getOrElse(fail("could not parse map")) shouldBe Map(
      0 -> MetadataValueStr("cardano"),
      1 -> MetadataValueLong(14),
      2 -> MetadataValueByteString(ByteString("2512a00e9653fe49a44a5886202e24d77eeb998f")))

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

  private final lazy val wallet = Wallet(
    id = "2512a00e9653fe49a44a5886202e24d77eeb998f",
    addressPoolGap = 20,
    balance = Balance(
      available = QuantityUnit(42000000, Units.lovelace),
      reward = QuantityUnit(42000000, Units.lovelace),
      total = QuantityUnit(42000000, Units.lovelace)
    ),
    delegation = Some(
      Delegation(
        active = DelegationActive(
          status = DelegationStatus.delegating,
          target = Some("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        ),
        next = List(
          DelegationNext(
            status = DelegationStatus.notDelegating,
            changesAt =
              Some(NextEpoch(epochStartTime = ZonedDateTime.parse("2020-01-22T10:06:39.037Z"), epochNumber = 14))
          )
        )
      )
    ),
    name = "Alan's Wallet",
    passphrase = Passphrase(lastUpdatedAt = ZonedDateTime.parse("2019-02-27T14:46:45.000Z")),
    state = SyncStatus(SyncState.ready, None),
    tip = networkTip
  )

  private final lazy val timedBlock = TimedBlock(
    time = ZonedDateTime.parse("2019-02-27T14:46:45.000Z"),
    block = Block(
      slotNumber = 1337,
      epochNumber = 14,
      height = QuantityUnit(1337, Units.block),
      absoluteSlotNumber = Some(8086)
    )
  )

  private final lazy val createdTransactionResponse = {
    val commonAmount = QuantityUnit(quantity = 42000000, unit = Units.lovelace)

    CreateTransactionResponse(
      id = "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1",
      amount = commonAmount,
      insertedAt = Some(timedBlock),
      pendingSince = Some(timedBlock),
      depth = Some(QuantityUnit(quantity = 1337, unit = Units.block)),
      direction = TxDirection.outgoing,
      inputs = Seq(inAddress),
      outputs = Seq(outAddress),
      withdrawals = Seq(
        StakeAddress(
          stakeAddress = "stake1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2x",
          amount = commonAmount
        )
      ),
      status = TxState.pending,
      metadata = Some(txMetadataOut)
    )
  }

  private final lazy val estimateFeeResponse = {
    val commonAmount = QuantityUnit(quantity = 42000000, unit = Units.lovelace)

    EstimateFeeResponse(estimatedMin = commonAmount, estimatedMax = commonAmount)
  }

  private final lazy val fundPaymentsResponse =
    FundPaymentsResponse(inputs = IndexedSeq(inAddress), outputs = Seq(outAddress))

  private final lazy val inAddress = InAddress(
    address = Some(addressIdStr),
    amount = Some(QuantityUnit(quantity = 42000000, unit = Units.lovelace)),
    id = "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1",
    index = 0
  )

  private final lazy val outAddress =
    OutAddress(address = addressIdStr, amount = QuantityUnit(quantity = 42000000, unit = Units.lovelace))

  private final lazy val addressIdStr =
    "addr1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g"

  private final lazy val txMetadataOut = TxMetadataOut(json = parse("""
                                                               |{
                                                               |      "0": {
                                                               |        "string": "cardano"
                                                               |      },
                                                               |      "1": {
                                                               |        "int": 14
                                                               |      },
                                                               |      "2": {
                                                               |        "bytes": "2512a00e9653fe49a44a5886202e24d77eeb998f"
                                                               |      },
                                                               |      "3": {
                                                               |        "list": [
                                                               |          {
                                                               |            "int": 14
                                                               |          },
                                                               |          {
                                                               |            "int": 42
                                                               |          },
                                                               |          {
                                                               |            "string": "1337"
                                                               |          }
                                                               |        ]
                                                               |      },
                                                               |      "4": {
                                                               |        "map": [
                                                               |          {
                                                               |            "k": {
                                                               |              "string": "key"
                                                               |            },
                                                               |            "v": {
                                                               |              "string": "value"
                                                               |            }
                                                               |          },
                                                               |          {
                                                               |            "k": {
                                                               |              "int": 14
                                                               |            },
                                                               |            "v": {
                                                               |              "int": 42
                                                               |            }
                                                               |          }
                                                               |        ]
                                                               |      }
                                                               |    }
                                                               |""".stripMargin).getOrElse(fail("Invalid metadata json")))

}