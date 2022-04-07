package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.model.Policy.{Kind, Script, asString}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PolicySpec extends AnyFlatSpec with Matchers {

  "Policy" should "be properly json encoded" in {

    val scripts = NonEmptyList.of(
      Script.Signature(KeyHash("key1")),
      Script.Signature(KeyHash("key2")),
      Script.Bound(100, after = true),
      Script.Bound(200, after = false)
    )

    def assert(kind: Policy.Kind): Assertion = {
      implicit val dummy: RandomTempFolder = RandomTempFolder(null)

      val expectedPolicy = Policy(scripts, kind)

      val actualPolicy = Policy.fromString(Policy.asString(expectedPolicy)).getOrElse(fail())

      assertResult(expectedPolicy.kind)(actualPolicy.kind)
      assertResult(expectedPolicy.scripts)(actualPolicy.scripts)
      assertResult(expectedPolicy)(actualPolicy)
    }

    List(Policy.Kind.All, Policy.Kind.AtLeast(10), Policy.Kind.Any).foreach(assert)
  }
}
