package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{CanRun, PaymentVerificationKey, PaymentVerificationKeyFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdAddressKeyHash(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with PaymentVerificationKeyFile
    with PaymentVerificationKey
    with CanRun {

  override type Out = CardanoCliCmdAddressKeyHash
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdAddressKeyHash = copy(b)
}
