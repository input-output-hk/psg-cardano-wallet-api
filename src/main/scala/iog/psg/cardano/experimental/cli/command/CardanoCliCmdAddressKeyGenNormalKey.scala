package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyGenNormalKey(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(): Int = exitValue()
}
