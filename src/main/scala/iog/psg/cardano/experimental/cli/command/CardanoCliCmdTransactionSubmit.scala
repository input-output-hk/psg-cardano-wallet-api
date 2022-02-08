package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{TestnetMagic, TxFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSubmit(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with TxFile {

  def run(): Int = exitValue()

  override type Out = CardanoCliCmdTransactionSubmit

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionSubmit = copy(b)
}
