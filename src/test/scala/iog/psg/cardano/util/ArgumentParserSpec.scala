package iog.psg.cardano.util


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
/**
 */
class ArgumentParserSpec extends AnyFlatSpec with Matchers {

  "ArgumentParser" should "handle params with values and without" in {

    val args = Array("-httpServer", "-rpcserver", "-password", "password", "-recovery", "-norecovery", "-myValue", "90")
    val sut = new ArgumentParser(args)
    assert(sut("-password") === Some("password"))
    assert(sut("-recovery") === None)
    assert(sut.contains("-recovery"))
    assert(sut.contains("-myValue"))
    assert(sut("-myValue") === Some("90"))
  }

  it should "handle 3" in {
    val args2 = Array("-httpServer", "-password", "password")
    val sut2 = new ArgumentParser(args2)
    assert(sut2.get("-password") == "password")
  }

  it should "handle 1" in {
    val args2 = Array("-httpServer")
    val sut2 = new ArgumentParser(args2)
    assert(sut2.contains("-httpServer"))
  }

  it should "handle 1 pair" in {
    val args2 = Array("-httpServer", "something")
    val sut2 = new ArgumentParser(args2)
    assert(sut2("-httpServer").contains("something"))
  }
}

