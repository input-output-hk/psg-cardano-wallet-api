package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyGenNormalKey(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(): Int = exitValue()
}