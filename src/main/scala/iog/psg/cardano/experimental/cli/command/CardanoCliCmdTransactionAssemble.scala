package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{OutFile, TxBodyFile, WitnessFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdTransactionAssemble(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxBodyFile
    with OutFile
    with WitnessFile {

  def run(implicit net: NetworkChooser): Int = exitValue

  override type Out = CardanoCliCmdTransactionAssemble

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionAssemble = copy(b)
}
