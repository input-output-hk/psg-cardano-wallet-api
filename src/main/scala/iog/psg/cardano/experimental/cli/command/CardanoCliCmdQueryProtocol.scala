package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdQueryProtocol(builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ChooseNetwork
    with ShelleyMode
    with MaryEra
    with OutFile
    with CanRun {

  override type Out = CardanoCliCmdQueryProtocol
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdQueryProtocol = copy(b)
}
