package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdAddressBuild(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ChooseNetwork
    with OutFile
    with PaymentVerificationKey
    with PaymentVerificationKeyFile
    with PaymentScriptFile
    with CanRun {

  override type Out = CardanoCliCmdAddressBuild
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdAddressBuild = copy(b)
}
