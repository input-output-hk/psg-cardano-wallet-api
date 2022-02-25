package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe._
import io.circe.syntax._


sealed trait Policy

object Policy {
  case class All(scripts: NonEmptyList[Script]) extends Policy
  case class Script(keyHash: String)

  implicit val encoder: Encoder[Policy] = {
    val allType = ("type", "all".asJson)
    val sigType = ("type", "sig".asJson)

    {
      case All(scripts) =>
        Json.obj(
          allType,
          ("scripts", scripts.map { s =>
            Json.obj(
              sigType,
              ("keyHash", s.keyHash.asJson)
            )
          }.asJson)
        )
    }
  }
}
