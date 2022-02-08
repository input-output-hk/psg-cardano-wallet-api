package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyHashString(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(): String = stringValue()
}
