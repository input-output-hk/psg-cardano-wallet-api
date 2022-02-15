package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, FileParam}

import java.io.File

trait PaymentVerificationKeyFile {
  this: CliCmdBuilder =>

  def paymentVerificationKeyFile(implicit fp: FileParam[PaymentVerificationKeyFile]): Out = {
    paymentVerificationKeyFile(fp.file)
  }

  def paymentVerificationKeyFile(value: File): Out =
    build(_.withParam("--payment-verification-key-file", value))
}
