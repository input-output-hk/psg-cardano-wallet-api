package iog.psg.cardano

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import iog.psg.cardano.CardanoApi.{CardanoApiRequest, CardanoApiResponse}

import scala.concurrent.{ExecutionContext, Future}

class ApiRequestExecutorImpl extends ApiRequestExecutor

sealed trait ApiRequestExecutor {

  def execute[T](request: CardanoApiRequest[T])(implicit ec: ExecutionContext, as: ActorSystem): Future[CardanoApiResponse[T]] =
    Http()
      .singleRequest(request.request)
      .flatMap(request.mapper)

}
