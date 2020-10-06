package iog.psg.cardano.util

import io.circe.Decoder
import io.circe.parser.decode
import org.scalatest.Assertions

import scala.io.Source

trait JsonFiles { self: Assertions =>

  final def getJsonFromFile(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    source.close()
    jsonStr
  }

  final def decodeJsonFile[T](file: String)(implicit dec: Decoder[T]) = {
    val jsonStr = getJsonFromFile(file)
    decode[T](jsonStr).getOrElse(fail(s"Could not decode $file"))
  }
}
