package iog.psg.cardano.experimental.cli.param


import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File


trait PaymentScriptFile {
  this: CliCmdBuilder =>

  def paymentScriptFile(file: File): Out = {
    build(_.withParam("--payment-script-file", file))
  }

}
