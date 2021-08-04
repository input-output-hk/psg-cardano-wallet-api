package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param.{TestnetMagic, TxFile}
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSubmit(protected val builder: ProcessBuilderHelper)
   extends CliCmd
    with TestnetMagic
    with CopyShim
    with TxFile {


  type CONCRETECASECLASS = CardanoCliCmdTransactionSubmit
  protected def copier = this

  def run() = exitValue()

}