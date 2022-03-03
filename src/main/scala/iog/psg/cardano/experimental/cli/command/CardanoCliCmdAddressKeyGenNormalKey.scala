package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.CanRun
import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyGenNormalKey(protected val builder: ProcessBuilderHelper)
  extends CliCmd with CanRun

