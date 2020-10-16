package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage}
import CardanoApiCodec.NetworkInfo
import io.circe.Encoder
import iog.psg.cardano.CardanoApiMain.CmdLine
import iog.psg.cardano.util.{ArgumentParser, DummyModel, ModelCompare, Trace}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}


class CardanoApiMainSpec extends AnyFlatSpec with Matchers with ModelCompare with DummyModel {

  "The Cmd Line -netInfo" should "show current network information" in new ApiRequestExecutorFixture[NetworkInfo]{
    override val expectedRequestUrl: String = "http://127.0.0.1:8090/v2/network/information"
    override val response: CardanoApiResponse[NetworkInfo] = Right(networkInfo)
    override val args: Array[String] = Array(CmdLine.netInfo)

    getTraceResults should include(""""status" : "ready"""")
  }

  it should "fail with exception during executing request" in new ApiRequestExecutorFixture[NetworkInfo] {
    override val expectedRequestUrl: String = "http://127.0.0.1:8090/v2/network/information"
    override val response: CardanoApiResponse[NetworkInfo] = Right(networkInfo)
    override val args: Array[String] = Array(CmdLine.netInfo)

    override implicit val apiExecutor = new ApiRequestExecutor {
      override def execute[T](request: CardanoApi.CardanoApiRequest[T])(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
        Future.failed(new RuntimeException("Test failed."))
      }.asInstanceOf[Future[CardanoApiResponse[T]]]
    }

    getTraceResults shouldBe "baseurl:http://127.0.0.1:8090/v2/, -netInfo, java.lang.RuntimeException: Test failed."
  }

  it should "return an API error" in new ApiRequestExecutorFixture[NetworkInfo] {
    override val expectedRequestUrl: String = "http://127.0.0.1:8090/v2/network/information"
    override val response: CardanoApiResponse[NetworkInfo] = Left(ErrorMessage("Test error.", "12345"))
    override val args: Array[String] = Array(CmdLine.netInfo)

    getTraceResults shouldBe "baseurl:http://127.0.0.1:8090/v2/, -netInfo, API Error message Test error., code 12345"
  }

  private sealed trait ApiRequestExecutorFixture[T] {
    val expectedRequestUrl: String
    val response: CardanoApiResponse[T]
    val args: Array[String]

    lazy val arguments = new ArgumentParser(args)

    private val traceResults: ArrayBuffer[String] = ArrayBuffer.empty

    implicit private val memTrace = new Trace {
      override def apply(s: String): Unit = traceResults += s
      override def close(): Unit = ()
    }

    implicit val apiExecutor = new ApiRequestExecutor {
      override def execute[T](request: CardanoApi.CardanoApiRequest[T])(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
        request.request.uri.toString() shouldBe expectedRequestUrl
        Future.successful(response)
      }.asInstanceOf[Future[CardanoApiResponse[T]]]
    }

    final def getTraceResults: String = {
      traceResults.clear()
      CardanoApiMain.run(arguments)
      traceResults.mkString(", ")
    }
  }
}
