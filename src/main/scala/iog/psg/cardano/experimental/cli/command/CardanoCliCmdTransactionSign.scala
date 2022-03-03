package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSign(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ChooseNetwork
    with SigningKeyFile
    with TxBodyFile
    with OutFile
    with CanRun {

  override type Out = CardanoCliCmdTransactionSign

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionSign = copy(b)
}
