package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{CanRun, ChooseNetwork, TxFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSubmit(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ChooseNetwork
    with TxFile
    with CanRun {

  override type Out = CardanoCliCmdTransactionSubmit

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionSubmit = copy(b)
}
