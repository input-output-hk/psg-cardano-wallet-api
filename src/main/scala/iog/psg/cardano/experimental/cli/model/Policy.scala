package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.api.{InFile, KeyType, Verification}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder

case class PolicyId(value: String) extends AnyVal

case class PolicyWithId(policy: Policy, policyId: PolicyId)

case class Policy(
  scripts: NonEmptyList[Policy.Script],
  kind: Policy.Kind
)(implicit
  val rootFolder: RandomTempFolder
) extends InFile {

  override val content: String = Policy.asString(this)
}

object Policy {

  sealed trait Kind
  object Kind {
    case object All extends Kind
    case object Any extends Kind
    case class AtLeast(value: Int) extends Kind
  }

  sealed trait Script
  object Script {
    case class Signature(keyHash: KeyHash[_ <: KeyType]) extends Script
    case class Bound(slot: Long, after: Boolean) extends Script
  }

  def asString(policy: Policy): String = codec(policy.rootFolder)(policy).noSpaces

  def fromString(value: String)(implicit f: RandomTempFolder): Either[Error, Policy] = {
    io.circe.parser
      .parse(value)
      .flatMap(codec(f).decodeJson)
  }

  implicit def codec(implicit f: RandomTempFolder): Codec[Policy] = {
    implicit val scriptCodec: Codec[Script] = {
      val typeField = "type"
      val keyHashField = "keyHash"
      val slotField = "slot"

      def scriptType(value: String): (String, Json) = (typeField, value.asJson)

      val sigType @ (_, sig) = scriptType("sig")
      val afterType @ (_, after) = scriptType("after")
      val beforeType @ (_, before) = scriptType("before")

      new Codec[Script] {

        override def apply(c: HCursor): Result[Script] = {
          c.downField(typeField)
            .as[Json]
            .flatMap {
              case `sig` =>
                c.downField(keyHashField)
                  .as[String]
                  .map(KeyHash[Verification])
                  .map(Policy.Script.Signature)

              case `after` =>
                c.downField(slotField)
                  .as[Int]
                  .map(Policy.Script.Bound(_, after = true))

              case `before` =>
                c.downField(slotField)
                  .as[Int]
                  .map(Policy.Script.Bound(_, after = false))

              case unknown =>
                Left(DecodingFailure(s"unexpected script type: $unknown", Nil))
            }
        }

        override def apply(policy: Script): Json = policy match {
          case Script.Signature(keyHash) =>
            Json.fromFields((keyHashField, keyHash.content.asJson) :: sigType :: Nil)

          case Script.Bound(slot, after) =>
            Json.fromFields(("slot", slot.asJson) :: (if (after) afterType else beforeType) :: Nil)
        }
      }
    }

    new Codec[Policy] {
      private val scriptsField = "scripts"
      private val typeField = "type"
      private val requiredField = "required"

      private val allType @ (_, all) = policyType("all")
      private val anyType @ (_, any) = policyType("any")
      private val atLeastType @ (_, atLeast) = policyType("atLeast")

      private def policyType(value: String): (String, Json) = (typeField, value.asJson)

      override def apply(c: HCursor): Result[Policy] = {
        for {
          scripts <- c.downField(scriptsField).as[NonEmptyList[Script]]

          kind <- {
            c.downField(typeField).as[Json].flatMap {
              case `all` => Right(Kind.All)
              case `any` => Right(Kind.Any)
              case `atLeast` => c.downField(requiredField).as[Int].map(Kind.AtLeast)
            }
          }

          policy = Policy(scripts, kind)
        } yield policy
      }

      override def apply(policy: Policy): Json = {
        Json.fromFields((scriptsField, policy.scripts.asJson) :: policyKind(policy.kind))
      }

      private def policyKind(value: Kind): List[(String, Json)] = {
        value match {
          case Kind.All => allType :: Nil
          case Kind.Any => anyType :: Nil
          case Kind.AtLeast(value) => atLeastType :: (requiredField, value.asJson) :: Nil
        }
      }
    }
  }
}
