package iog.psg.external

import akka.actor.ActorSystem
import io.circe.jawn.decode
import io.circe.syntax._
import iog.psg.cardano.CardanoApiCodec.ImplicitCodecs._
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.{ CustomPatienceConfiguration, DummyModel, JsonFiles }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * NB It's import that this class is in a package that is 'external' to the
 * cardano package in order to test that these encoders and decoders are
 * available to clients and are not privately scoped to the implementation package
 * (using private[cardano])
 */
class CardanoApiCodecSpec
    extends AnyFlatSpec
    with Matchers
    with DummyModel
    with JsonFiles
    with ScalaFutures
    with CustomPatienceConfiguration {

  implicit val as: ActorSystem = ActorSystem("cardano-api-test-system")

  "Encode class with Nones" should "drop nulls in Delegation" in {
    val delAct = DelegationActive(status = DelegationStatus.delegating, target = None)
    val delNext = DelegationNext(status = DelegationStatus.delegating, changesAt = None)
    val del = Delegation(delAct, List(delNext))
    del.asJson.noSpaces shouldBe """{"active":{"status":"delegating"},"next":[{"status":"delegating"}]}""".stripMargin
  }

  it should "drop nulls in NetworkTip" in {
    val networkTip = NetworkTip(epochNumber = 1, slotNumber = 2, height = None, absoluteSlotNumber = None)
    networkTip.asJson.noSpaces shouldBe """{"epoch_number":1,"slot_number":2}"""
  }

  it should "drop nulls in NodeTip" in {
    val nt = nodeTip.copy(absoluteSlotNumber = None)
    nt.asJson.noSpaces shouldBe """{"height":{"quantity":1337,"unit":"block"},"slot_number":1337,"epoch_number":14}"""
  }

  it should "drop nulls in SyncStatus" in {
    val ss = SyncStatus(SyncState.ready, None)
    ss.asJson.noSpaces shouldBe """{"status":"ready"}"""
  }

  it should "drop nulls in wallet" in {
    val walletWithNones = wallet.copy(delegation = None)

    val delegation = walletWithNones.asJson.\\("delegation").headOption
    delegation shouldBe None
  }

  it should "drop nulls in created transaction response" in {
    val ctr = CreateTransactionResponse(
      id = "123",
      amount = QuantityUnit(42000000, Units.lovelace),
      insertedAt = None,
      pendingSince = None,
      depth = None,
      direction = TxDirection.incoming,
      inputs = Nil,
      outputs = Nil,
      withdrawals = Nil,
      status = TxState.inLedger,
      metadata = None
    )

    ctr.asJson.noSpaces shouldBe """{"id":"123","amount":{"quantity":42000000,"unit":"lovelace"},"direction":"incoming","inputs":[],"outputs":[],"withdrawals":[],"status":"in_ledger"}"""
  }

  it should "drop nulls in CreateRestore" in {
    val cr = CreateRestore(
      name = "abc",
      passphrase = "123",
      mnemonicSentence = mnemonicSentence.mnemonicSentence,
      mnemonicSecondFactor = None,
      addressPoolGap = None
    )

    cr.asJson.noSpaces shouldBe """{"name":"abc","passphrase":"123","mnemonic_sentence":["a","b","c","d","e","a","b","c","d","e","a","b","c","d","e"]}"""
  }

  it should "drop nulls Block" in {
    val b = Block(
      slotNumber = 1,
      epochNumber = 2,
      height = QuantityUnit(42000000, Units.lovelace),
      absoluteSlotNumber = None
    )

    b.asJson.noSpaces shouldBe """{"slot_number":1,"epoch_number":2,"height":{"quantity":42000000,"unit":"lovelace"}}"""
  }

  "Quantity" should "be decoded to Double" in {
    val decodedQU = decode[QuantityUnit[Double]]("""{"quantity":123.45,"unit":"lovelace"}""")
    decodedQU.getOrElse(fail("Not decoded")).quantity shouldBe 123.45
  }

  it should "be decoded to Long" in {
    val decodedQU = decode[QuantityUnit[Long]]("""{"quantity":123,"unit":"lovelace"}""")
    decodedQU.getOrElse(fail("Not decoded")).quantity shouldBe 123
  }

  it should "be encoded to Double" in {
    val qu = QuantityUnit(123.45, Units.lovelace)
    qu.asJson.noSpaces shouldBe """{"quantity":123.45,"unit":"lovelace"}"""
  }

  it should "be encoded to Long" in {
    val qu = QuantityUnit(123, Units.lovelace)
    qu.asJson.noSpaces shouldBe """{"quantity":123,"unit":"lovelace"}"""
  }

  "Decode transactions" should "decode huge file" in {
    val jsonFileTxs =
      decodeViaStream(file = "transactions_huge.json", jsonPath = "$[*]").futureValue
        .map(_.utf8String)
        .map(jsonStr =>
          decode[CreateTransactionResponse](jsonStr).getOrElse(fail(s"Could not decode $jsonStr"))
        )

    jsonFileTxs.size shouldBe 701
    jsonFileTxs.head.id shouldBe "1b38b632d8fd9575bb669900046c089807f2437307b37e0cec0f7d83a6d02869"
    jsonFileTxs.last.id shouldBe "d7b3efda1d44daf481937de314d2a25fe301c509c95acfbb215b413856b9730b"
  }

}
