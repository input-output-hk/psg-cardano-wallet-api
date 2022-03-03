package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{CanRun, ChooseNetwork, OutFile, ShelleyMode}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdQueryUtxo(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ShelleyMode
    with ChooseNetwork
    with OutFile
    with CanRun {

  def address(address: String): CardanoCliCmdQueryUtxo =
    copy(builder.withParam("--address", address))

  override type Out = CardanoCliCmdQueryUtxo
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdQueryUtxo = copy(b)
}
