package iog.psg.cardano.util

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Source => AkkaSource }
import akka.stream.Materializer
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import io.circe.Decoder
import io.circe.parser.decode
import iog.psg.cardano.CardanoApiCodec._
import org.scalatest.Assertions
import org.scalatest.concurrent.ScalaFutures

import scala.io.Source

trait JsonFiles { self: Assertions with ScalaFutures with CustomPatienceConfiguration =>

  implicit val system: ActorSystem = ActorSystem("test")

  final lazy val jsonFileWallet = decodeJsonFile[Wallet]("wallet.json")
  final lazy val jsonFileCreatedTransactionResponse = decodeJsonFile[CreateTransactionResponse]("transaction.json")
  final lazy val jsonFileCreatedTransactionsResponse =
    decodeJsonFile[Seq[CreateTransactionResponse]]("transactions.json")
  final lazy val jsonFileCreatedTransactionsHugeResponse =
    decodeViaStream(file = "transactions_huge.json", jsonPath = "$[*]").futureValue
      .map(_.utf8String)
      .map(jsonStr => decode[CreateTransactionResponse](jsonStr).getOrElse(fail(s"Could not decode $jsonStr")))

  decodeJsonFile[Seq[CreateTransactionResponse]]("transactions_huge.json")

  final def getJsonFromFile(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    source.close()
    jsonStr
  }

  private def decodeViaStream(file: String, jsonPath: String)(implicit mat: Materializer) = {
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
