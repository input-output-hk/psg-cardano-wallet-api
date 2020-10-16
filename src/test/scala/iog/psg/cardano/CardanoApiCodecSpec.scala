package iog.psg.cardano

import io.circe.jawn.decode
import io.circe.syntax._
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.DummyModel
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiCodecSpec extends AnyFlatSpec with Matchers with DummyModel {

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
      slotNumber = 1, epochNumber = 2, height = QuantityUnit(42000000, Units.lovelace), absoluteSlotNumber = None
    )

    b.asJson.noSpaces shouldBe """{"slot_number":1,"epoch_number":2,"height":{"quantity":42000000,"unit":"lovelace"}}"""
  }

  "Quantity" should "be decoded to Long" in {
    val decodedQU = decode[QuantityUnit]("""{"quantity":123.45,"unit":"lovelace"}""")
    decodedQU.getOrElse(fail("Not decoded")).quantity shouldBe 123
  }

}
