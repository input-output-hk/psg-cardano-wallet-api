package iog.psg.cardano

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest

object Main {

  val baseUri = "http://localhost:8090/v2/"
  val wallet = "wallets"

  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val context = system.executionContext

  def main(args: Array[String]): Unit = {
    val response = Http().singleRequest(HttpRequest(uri = baseUri + wallet))

    response.map(resp => {
      println(resp)
      System.exit(0)
    })

  }

}
