package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.api.Verification
import iog.psg.cardano.experimental.cli.model.Policy.Script.{Bound, Signature}
import iog.psg.cardano.experimental.cli.model.Policy.{Kind, Script, asString}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder
import org.scalatest.{Assertion, EitherValues}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PolicySpec extends AnyFlatSpec with Matchers with EitherValues {

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

  "A DISH policy by string" should "parse" in {

    val keyHash = "3e6ea29f537e0783f469077723b9ef6f740993e84f616db36ad355a9"
    val slot = 56894689

    val dish =
      s"""{\n      \"type\": \"all\",\n      \"scripts\":\n      [\n        {\n          \"type\": \"after\",\n          \"slot\": ${slot}\n        },\n        {\n          \"type\": \"sig\",\n          \"keyHash\": \"${keyHash}\"\n        }\n      ]\n    }"""
    implicit val dummy: RandomTempFolder = RandomTempFolder(null)

    val policyOrError = Policy.fromString(dish)

    val policy = policyOrError.value

    policy.kind shouldBe Policy.Kind.All
    policy.scripts.toList should contain (Signature(KeyHash[Verification](keyHash)))
    policy.scripts.toList should contain (Bound(slot, after = true))
    policy.scripts.toList.size shouldBe 2
  }
}
