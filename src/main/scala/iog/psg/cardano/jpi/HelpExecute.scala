package iog.psg.cardano.jpi

import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps.{CardanoApiRequestFOps, CardanoApiRequestOps}
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.javaapi.FutureConverters


class CardanoApiException(message: String, code: String) extends Exception(s"Message: $message, Code: $code")

class HelpExecute(implicit ec: ExecutionContext, as: ActorSystem) {

  @throws(classOf[CardanoApiException])
  private def unwrapResponse[T](resp: CardanoApiResponse[T]): T = resp match {
    case Right(t) => t
    case Left(ErrorMessage(message, code)) =>
      throw new CardanoApiException(message, code)
  }

  @throws(classOf[CardanoApiException])
  def execute[T](request: iog.psg.cardano.CardanoApi.CardanoApiRequest[T]): CompletionStage[T] = {
    FutureConverters.asJava(request.execute.map(unwrapResponse))
  }

  @throws(classOf[CardanoApiException])
  def execute[T](request: Future[iog.psg.cardano.CardanoApi.CardanoApiRequest[T]]): CompletionStage[T] = {
    FutureConverters.asJava(request.execute.map(unwrapResponse))
  }

  def toScalaImmutable[B](in: java.util.Map[java.lang.Long,String]): Map[java.lang.Long, String] = in.asScala.toMap

}
