package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{OutFile, PaymentScriptFile, TestnetMagic}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdAddressBuildScript(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with PaymentScriptFile
    with OutFile {

  override type Out = CardanoCliCmdAddressBuildScript
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdAddressBuildScript = copy(b)
}
