package iog.psg.cardano.jpi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 */
class CardanoJpiSpec extends AnyFlatSpec with Matchers {

  "ArgumentParser" should "handle params with values and without" in {
    NetworkInfoTest.get()
  }

}

