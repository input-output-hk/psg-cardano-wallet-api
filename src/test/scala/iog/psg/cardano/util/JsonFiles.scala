package iog.psg.cardano.util

import akka.stream.Materializer
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.{Sink, Source => AkkaSource}
import akka.util.ByteString
import io.circe.Decoder
import io.circe.parser.decode
import iog.psg.cardano.CardanoApiCodec._
import org.scalatest.Assertions

import scala.io.Source

trait JsonFiles { self: Assertions =>

  final lazy val jsonFileWallet = decodeJsonFile[Wallet]("wallet.json")
  final lazy val jsonFileCreatedTransactionResponse = decodeJsonFile[CreateTransactionResponse]("transaction.json")
  final lazy val jsonFileCreatedTransactionsResponse =
    decodeJsonFile[Seq[CreateTransactionResponse]]("transactions.json")

  final def getJsonFromFile(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    source.close()
    jsonStr
  }

  final def decodeViaStream(file: String, jsonPath: String)(implicit mat: Materializer) = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    AkkaSource
      .single(ByteString.fromString(jsonStr))
      .via(JsonReader.select(jsonPath))
      .runWith(Sink.seq)
  }

  final def decodeJsonFile[T](file: String)(implicit dec: Decoder[T]): T = {
    val jsonStr = getJsonFromFile(file)
    decode[T](jsonStr).getOrElse(fail(s"Could not decode $file"))
  }
}
