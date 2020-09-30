package iog.psg.cardano.util

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.{ApiRequestExecutor, CardanoApi}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class ApiExecutorOverrideSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  private implicit val system = ActorSystem("SingleRequest")
  import system.dispatcher

  "A client in different package" should "be able to override the APIExecutor" in {
    //This will not compile if the trait is sealed.
    val testUri = "http://localhost:9999/"
    val response = Left(ErrorMessage("TESTAPIOVERRIDE", "TESTAPIOVERRIDE"))
    val sut = new ApiRequestExecutor {
      override def execute[T](request: CardanoApi.CardanoApiRequest[T])(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] = {
        request.request.uri.toString() shouldBe testUri
        Future.successful(response)
      }
    }


    val result = sut.execute(CardanoApiRequest(
      HttpRequest(uri = testUri),
      _ => Future.successful(response)
    )).futureValue

    result shouldBe response
  }
}
