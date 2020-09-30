package iog.psg.cardano.jpi

import java.util.concurrent.CompletionStage

import iog.psg.cardano.ApiRequestExecutor
import iog.psg.cardano.jpi.{ApiRequestExecutor => JApiRequestExecutor}
import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps.CardanoApiRequestOps
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage}
import iog.psg.cardano.CardanoApiCodec.{MetadataValue, MetadataValueStr}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.javaapi.FutureConverters


class CardanoApiException(message: String, code: String) extends Exception(s"Message: $message, Code: $code")

object HelpExecute {

  def toScalaImmutable[B](in: java.util.Map[java.lang.Long, String]): Map[java.lang.Long, String] = in.asScala.toMap

  def toMetadataMap(in: java.util.Map[java.lang.Long, String]): Map[Long, MetadataValue] = {
    in.asScala.map {
      case (k, v) => k.toLong -> MetadataValueStr (v)
    }
  }.toMap
}

class HelpExecute(implicit ec: ExecutionContext, as: ActorSystem) extends JApiRequestExecutor {

  implicit val executor: ApiRequestExecutor = ApiRequestExecutor

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
    FutureConverters.asJava(request).thenCompose(request => this.execute(request))
  }

  def toScalaImmutable[B](in: java.util.Map[java.lang.Long, String]): Map[java.lang.Long, String] =
    HelpExecute.toScalaImmutable(in)

}
