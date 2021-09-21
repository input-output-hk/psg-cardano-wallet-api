package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}
import iog.psg.cardano.experimental.cli.param._

case class CardanoCliCmdTransactionWitness(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with CopyShim
    with TxBodyFile
    with OutFile
    with TestnetMagic
    with ScriptFile
    with SigningKeyFile {

  override type CONCRETECASECLASS = CardanoCliCmdTransactionWitness
  val copier = this

  def run(): Int = exitValue()
}