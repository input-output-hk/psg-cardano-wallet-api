package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait PaymentVerificationKeyFile {
  this: CliCmdBuilder =>

  def paymentVerificationKeyFile(value: File): Out =
    withParam("--payment-verification-key-file", value)
}
