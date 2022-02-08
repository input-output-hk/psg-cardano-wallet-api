package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdAddressBuild(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with OutFile
    with PaymentVerificationKey
    with PaymentVerificationKeyFile
    with PaymentScriptFile {

  def res(): String = run[String]

  override type Out = CardanoCliCmdAddressBuild
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdAddressBuild = copy(b)
}
