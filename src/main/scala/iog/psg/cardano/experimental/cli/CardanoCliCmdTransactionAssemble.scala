package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}
import iog.psg.cardano.experimental.cli.param._

case class CardanoCliCmdTransactionAssemble(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with TxBodyFile
    with OutFile
    with WitnessFile
    with CopyShim {

  override type CONCRETECASECLASS = CardanoCliCmdTransactionAssemble
  val copier = this

  def run(): Int = exitValue()
}
