package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

trait PaymentVerificationKey {
  this: CliCmdBuilder =>

  def paymentVerificationKey(value: String): Out =
    withParam("--payment-verification-key", value)
}
