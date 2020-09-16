package iog.psg.cardano.util

import iog.psg.cardano.util.StringToMetaMapParser.toMetaMap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 */
class StringToMapParserSpec extends AnyFlatSpec with Matchers {

  "MetaMapParser" should "parse simple map" in {
    toMetaMap(Some("1:a:2:b")) shouldBe Some(Map((1 -> "a"), 2 -> "b"))
  }

  it should "correctly parse an empty string" in {
    toMetaMap(Some("")) shouldBe None
  }

  it should "fail on unbalanced input (no value for key)" in {
    an [RuntimeException] shouldBe thrownBy( toMetaMap(Some("1:a:2")))
  }


  it should "fail and show all bad keys if key not type 'long'" in {
    val ex = the [RuntimeException] thrownBy( toMetaMap(Some("1:a:a:b:g:r")))
    ex.toString should include ("a, g")
  }
}

