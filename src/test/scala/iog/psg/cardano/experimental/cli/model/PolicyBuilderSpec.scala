package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.api.Verification
import iog.psg.cardano.experimental.cli.model.Policy.Script.{Bound, Signature}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Path

class PolicyBuilderSpec extends AnyFlatSpec with Matchers with EitherValues {

  implicit val rootFolder: RandomTempFolder = RandomTempFolder(Path.of("."))

  "PolicyBuilder" should "fail if no signature" in {

    intercept[IllegalArgumentException] {
      PolicyBuilder().build
    }

    intercept[IllegalArgumentException] {
      PolicyBuilder().withAfterConstraint(3).build
    }

  }

  it should "create a minimal policy with one key" in {
    val kh = KeyHash[Verification]("somestring")
    val p = PolicyBuilder().withSignatureOf(kh).build
    p.scripts.toList shouldBe List(Signature(kh))
    p.kind shouldBe Policy.Kind.All
  }

  it should "create a minimal policy (default kind ALL sigs required) with one key" in {
    val kh = KeyHash[Verification]("somestring")
    val p = PolicyBuilder().withSignatureOf(kh).build
    p.scripts.toList shouldBe List(Signature(kh))
    p.kind shouldBe Policy.Kind.All
  }

  it should "respect the `kind`" in {
    val kh = KeyHash[Verification]("somestring")
    var p = PolicyBuilder().withSignatureOf(kh).withAllSigsRequired().build

    p.scripts.toList shouldBe List(Signature(kh))
    p.kind shouldBe Policy.Kind.All

    p = PolicyBuilder().withSignatureOf(kh).withAnySigRequired().build

    p.scripts.toList shouldBe List(Signature(kh))
    p.kind shouldBe Policy.Kind.Any

    p = PolicyBuilder().withSignatureOf(kh).withAtLeastSigsRequired(2).build

    p.scripts.toList shouldBe List(Signature(kh))
    p.kind shouldBe Policy.Kind.AtLeast(2)
  }

  it should "aggregrate signatures" in {
    val kh = KeyHash[Verification]("somestring")
    val kh2 = KeyHash[Verification]("somestring2")
    val p = PolicyBuilder()
      .withSignatureOf(kh)
      .withSignatureOf(kh2)
      .build

    p.scripts.toList should contain (Signature(kh))
    p.scripts.toList should contain (Signature(kh2))

  }

  it should "respect slot bounds" in {
    val kh = KeyHash[Verification]("somestring")

    val p = PolicyBuilder()
      .withSignatureOf(kh)
      .withAfterConstraint(400)
      .withBeforeConstraint(800)
      .build

    p.scripts.toList should contain (Signature(kh))
    p.scripts.toList should contain (Bound(400, after = true))
    p.scripts.toList should contain (Bound(800, after = false))
    p.scripts.toList.size shouldBe(3)
  }

}
