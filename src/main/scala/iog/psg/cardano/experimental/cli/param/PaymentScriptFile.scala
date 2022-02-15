package iog.psg.cardano.experimental.cli.param


import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, FileParam}

import java.io.File
import java.nio.file.Files


trait PaymentScriptFile {
  this: CliCmdBuilder =>

  def paymentScriptFile(file: File): Out = {
    build(_.withParam("--payment-script-file", file))
  }


  def paymentScriptFile(implicit fp: FileParam[PaymentScriptFile]): Out = {
    paymentScriptFile(fp.file)
  }
}
