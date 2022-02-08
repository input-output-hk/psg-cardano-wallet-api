package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.ops.CardanoCliSyntax
import iog.psg.cardano.experimental.nativeassets.PolicySyntax

object implicits
  extends CardanoCliSyntax
    with PolicySyntax
