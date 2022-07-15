package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.api.KeyType
import iog.psg.cardano.experimental.cli.model.Policy.Script
import iog.psg.cardano.experimental.cli.util.RandomTempFolder

case class PolicyBuilder(
  private val scripts: Seq[Script] = Seq.empty,
  private val kind: Policy.Kind = Policy.Kind.All
) {

  def withAllSigsRequired(): PolicyBuilder = this.copy(kind = Policy.Kind.All)
  def withAnySigRequired(): PolicyBuilder  = this.copy(kind = Policy.Kind.Any)

  def withAtLeastSigsRequired(numberOfSigsRequired: Int): PolicyBuilder =
    this.copy(kind = Policy.Kind.AtLeast(numberOfSigsRequired))

  def withSignatureOf(keyHash: KeyHash[_ <: KeyType]): PolicyBuilder =
    this.copy(scripts = scripts :+ Policy.Script.Signature(keyHash))


  def withBeforeConstraint(slot: Long): PolicyBuilder =
    this.copy(scripts = scripts.filter {
      case Policy.Script.Bound(_, false) => false
      case _ => true
    } :+ Policy.Script.Bound(slot, after = false))

  def withAfterConstraint(slot: Long): PolicyBuilder =
    this.copy(scripts = scripts.filter {
      case Policy.Script.Bound(_, true) => false
      case _ => true
    } :+ Policy.Script.Bound(slot, after = true))

  def build(implicit rootFolder: RandomTempFolder): Policy = {
    require(
      scripts.exists {
        case _: Script.Signature => true
        case _                   => false
      },
      "There must be a at least one script of type Signature!"
    )

    Policy(NonEmptyList.of[Policy.Script](scripts.head, scripts.tail: _*), kind)

  }
}
