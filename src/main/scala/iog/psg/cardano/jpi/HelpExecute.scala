package iog.psg.cardano.jpi

import java.util.concurrent.{CompletionStage, TimeUnit}

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps.{CardanoApiRequestFOps, CardanoApiRequestOps}
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage, IOExecutionContext, defaultMaxWaitTime}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.javaapi.FutureConverters


class CardanoApiException(message: String, code: String) extends Exception(s"Message: $message, Code: $code")

class HelpExecute(implicit ioEc: IOExecutionContext, ec: ExecutionContext, as: ActorSystem) {

  @throws(classOf[CardanoApiException])
  private def unwrapResponse[T](resp: CardanoApiResponse[T]): T = resp match {
    case Right(t) => t
    case Left(ErrorMessage(message, code)) => throw new CardanoApiException(message, code)
  }

  @throws(classOf[CardanoApiException])
  def execute[T](request: iog.psg.cardano.CardanoApi.CardanoApiRequest[T]): CompletionStage[T] = {
    FutureConverters.asJava(request.execute.map(unwrapResponse))
  }

  @throws(classOf[CardanoApiException])
  def execute[T](request: Future[iog.psg.cardano.CardanoApi.CardanoApiRequest[T]]): CompletionStage[T] = {
    FutureConverters.asJava(request.execute.map(unwrapResponse))
  }

  @throws(classOf[CardanoApiException])
  def executeBlocking[T](
                          request: iog.psg.cardano.CardanoApi.CardanoApiRequest[T]
                        ): T = {
    unwrapResponse(request.executeBlocking)
  }

  @throws(classOf[CardanoApiException])
  def executeBlocking[T](
                          request: iog.psg.cardano.CardanoApi.CardanoApiRequest[T],
                          maxWaitMilliSeconds: Long
                        ): T = {
    unwrapResponse(request.executeBlocking(FiniteDuration(maxWaitMilliSeconds, TimeUnit.MILLISECONDS)))
  }

}
