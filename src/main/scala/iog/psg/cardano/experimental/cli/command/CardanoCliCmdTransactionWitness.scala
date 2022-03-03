package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionWitness(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxBodyFile
    with OutFile
    with ChooseNetwork
    with ScriptFile
    with SigningKeyFile
    with CanRun {

  override type Out = CardanoCliCmdTransactionWitness

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionWitness = copy(b)
}
