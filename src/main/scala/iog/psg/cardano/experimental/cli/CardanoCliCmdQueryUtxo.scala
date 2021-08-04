package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}
import iog.psg.cardano.experimental.cli.param._

import java.io.File

case class CardanoCliCmdQueryUtxo(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with ShelleyMode
    with CopyShim
    with TestnetMagic {

  def address(address:String): CardanoCliCmdQueryUtxo =
    copy(builder = builder.withParam("--address", address))

  def outFile(outFile: File): CardanoCliCmdQueryUtxo = {
    copy(builder.withParam("--out-file", outFile))
  }

  def run(): Int = exitValue()

  override type CONCRETECASECLASS = CardanoCliCmdQueryUtxo
  override protected def copier: CardanoCliCmdQueryUtxo = this
}