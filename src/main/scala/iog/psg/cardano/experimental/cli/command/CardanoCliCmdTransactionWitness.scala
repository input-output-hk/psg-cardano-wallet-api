package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdTransactionWitness(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxBodyFile
    with OutFile
    with TestnetMagic
    with ScriptFile
    with SigningKeyFile {

  def run(implicit net: NetworkChooser): Int = exitValue

  override type Out = CardanoCliCmdTransactionWitness

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionWitness = copy(b)
}
