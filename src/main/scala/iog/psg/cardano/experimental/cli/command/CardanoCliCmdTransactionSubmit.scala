package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{TestnetMagic, TxFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSubmit(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with TxFile {

  def run(implicit net: NetworkChooser): Int = exitValue

  override type Out = CardanoCliCmdTransactionSubmit

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionSubmit = copy(b)
}
