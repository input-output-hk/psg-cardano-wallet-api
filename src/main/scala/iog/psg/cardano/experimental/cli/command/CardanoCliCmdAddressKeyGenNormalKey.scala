package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyGenNormalKey(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(implicit net: NetworkChooser): Int = exitValue
}
