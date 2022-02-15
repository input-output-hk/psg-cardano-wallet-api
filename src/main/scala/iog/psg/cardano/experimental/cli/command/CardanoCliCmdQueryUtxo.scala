package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{OutFile, ShelleyMode, TestnetMagic}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

case class CardanoCliCmdQueryUtxo(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ShelleyMode
    with TestnetMagic
    with OutFile {

  def address(address: String): CardanoCliCmdQueryUtxo =
    copy(builder.withParam("--address", address))

  def run(implicit net: NetworkChooser): Int = exitValue

  override type Out = CardanoCliCmdQueryUtxo
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdQueryUtxo = copy(b)
}
