package iog.psg.cardano

import iog.psg.cardano.util._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CantApiSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with ResourceFiles
    with CustomPatienceConfiguration {

  "Dummy" should "do it" in {
    val t = ConfigureFactory.config.getConfig("cardano")
    println(t)
  }

}
