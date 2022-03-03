package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{CanRun, OutFile, TxBodyFile, WitnessFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionAssemble(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxBodyFile
    with OutFile
    with WitnessFile
    with CanRun {

  override type Out = CardanoCliCmdTransactionAssemble

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionAssemble = copy(b)
}
