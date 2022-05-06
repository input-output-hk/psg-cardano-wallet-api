package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{CanRun, ChooseNetwork, OutFile, ShelleyMode}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdQueryTip(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ChooseNetwork
    with OutFile
    with CanRun {

  override type Out = CardanoCliCmdQueryTip
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdQueryTip = copy(b)
}
