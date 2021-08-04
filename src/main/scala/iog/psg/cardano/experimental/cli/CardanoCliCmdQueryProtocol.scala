package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param.{MaryEra, OutFile, ShelleyMode, TestnetMagic}
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdQueryProtocol(builder: ProcessBuilderHelper)
  extends CliCmd
    with TestnetMagic
    with ShelleyMode
    with OutFile
    with CopyShim {

  override type CONCRETECASECLASS = CardanoCliCmdQueryProtocol
  val copier = this

  def string(): String = stringValue()

  def run(): Seq[String] = {
    allValues()
  }
}
