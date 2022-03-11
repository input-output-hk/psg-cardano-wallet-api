package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe._
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.api.{InFile, KeyType}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder

case class PolicyId(value: String) extends AnyVal

sealed trait Policy extends InFile

object Policy {

  case class All(scripts: NonEmptyList[Script])(implicit val rootFolder: RandomTempFolder) extends Policy {
    override val content: String = Policy.asString(this)
  }

  case class Script(keyHash: KeyHash[_ <: KeyType])

  def asString(policy: Policy): String = policy.asJson.noSpaces

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
              ("keyHash", s.keyHash.content.asJson)
            )
          }.asJson)
        )
    }
  }
}
