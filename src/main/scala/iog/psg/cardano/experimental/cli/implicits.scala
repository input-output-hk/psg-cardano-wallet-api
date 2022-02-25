package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.model.{Policy, PolicyOps}
import iog.psg.cardano.experimental.cli.ops.CardanoCliSyntax

trait PolicySyntax {

  implicit def toPolicyOps(policy: Policy): PolicyOps =
    new PolicyOps(policy)
}

object implicits
  extends CardanoCliSyntax
    with PolicySyntax
