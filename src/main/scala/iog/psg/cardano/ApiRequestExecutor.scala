package iog.psg.cardano

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse}

import scala.concurrent.{ExecutionContext, Future}

object ApiRequestExecutor extends ApiRequestExecutor

trait ApiRequestExecutor {

  def execute[T](request: CardanoApiRequest[T])(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] =
    Http()
      .singleRequest(request.request)
      .flatMap(request.mapper)

}
