package iog.psg.cardano

import io.circe.syntax.EncoderOps
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.DummyModel
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardnanoApiCodecSpec extends AnyFlatSpec with Matchers with DummyModel {

  "Encode class with Nones" should "drop nulls in Delegation" in  {
    val delAct = DelegationActive(
      status = DelegationStatus.delegating,
      target = None
    )
    val delNext = DelegationNext(
      status = DelegationStatus.delegating,
      changesAt = None
    )
    val del = Delegation(delAct, List(delNext))
    del.asJson.noSpaces shouldBe """{"active":{"status":"delegating"},"next":[{"status":"delegating"}]}""".stripMargin
  }

  it should "drop nulls in NetworkTip" in {
    val networkTip = NetworkTip(
      epochNumber = 1, slotNumber = 2, height = None, absoluteSlotNumber = None
    )
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

}