package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiResponse
import iog.psg.cardano.CardanoApiCodec.NetworkInfo
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

    getTraceResults shouldBe "baseurl:http://127.0.0.1:8090/v2/, -netInfo, NetworkInfo(SyncStatus(ready,None),NetworkTip(14,1337,None,Some(8086)),NodeTip(QuantityUnit(1337,block),1337,14,Some(8086)),NextEpoch(2000-01-02T03:04:05Z,14))"
  }

  private sealed trait ApiRequestExecutorFixture[T] {
    val expectedRequestUrl: String
    val response: CardanoApiResponse[T]
    val args: Array[String]

    lazy val arguments = new ArgumentParser(args)

    private val traceResults: ArrayBuffer[String] = ArrayBuffer.empty

    implicit private val memTrace = new Trace {
      override def apply(s: Object): Unit = traceResults += s.toString
      override def close(): Unit = ()
    }

    implicit private val apiExecutor = new ApiRequestExecutor {
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
