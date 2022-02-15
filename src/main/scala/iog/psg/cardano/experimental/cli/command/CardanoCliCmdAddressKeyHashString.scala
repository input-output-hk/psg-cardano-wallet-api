package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyHashString(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(implicit net: NetworkChooser): String = stringValue
}
