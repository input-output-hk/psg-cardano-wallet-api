package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, FileParam}


import java.io.File

trait OutFile {
  self: CliCmdBuilder =>

  def outFile(implicit txBody: FileParam[OutFile]): Out = {
    outFile(txBody.file)
  }

  def outFile(txBody: File): Out =
    build(_.withParam("--out-file", txBody))
}
