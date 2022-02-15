package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{MaryEra, OutFile, ShelleyMode, TestnetMagic}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdQueryProtocol(builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with ShelleyMode
    with MaryEra
    with OutFile {

  def res(implicit net: NetworkChooser): String = run[String]

  override type Out = CardanoCliCmdQueryProtocol
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdQueryProtocol = copy(b)
}
