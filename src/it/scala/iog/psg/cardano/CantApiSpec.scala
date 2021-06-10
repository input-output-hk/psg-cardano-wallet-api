package iog.psg.cardano

import iog.psg.cardano.util._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class CantApiSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with ResourceFiles
    with CustomPatienceConfiguration {

  "Dummy" should "do it" in {

    ConfigureFactory.config.getString("cardano.wallet1.passphrase").split(" ").foreach(println)
    println("END p1")

    ConfigureFactory.config.getString("cardano.wallet3.passphrase").split(" ").foreach(println)
    println(" p3 END")

    val t = ConfigureFactory.config.getConfig("cardano")
    val f = new File("crap.txt")
    Files.write(Paths.get("crap.txt"), t.toString.getBytes(StandardCharsets.UTF_8))
    assert(f.exists(), "where is the file?")

    ConfigureFactory.config.getString("cardano.wallet1.mnemonic")
    println(f.getPath)
  }

}
