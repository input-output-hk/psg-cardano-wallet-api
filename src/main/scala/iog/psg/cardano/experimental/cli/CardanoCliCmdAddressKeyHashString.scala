package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyHashString(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def run(): String = stringValue()
}