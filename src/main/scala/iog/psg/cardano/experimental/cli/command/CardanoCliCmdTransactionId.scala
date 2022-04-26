package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionId(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxFile
    with TxBodyFile
    with CanRun {

  override type Out = CardanoCliCmdTransactionId

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionId = copy(b)
}
