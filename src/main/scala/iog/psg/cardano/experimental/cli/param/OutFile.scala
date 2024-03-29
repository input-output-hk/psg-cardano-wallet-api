package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait OutFile {
  self: CliCmdBuilder =>

  def outFile(txBody: File): Out =
    withParam("--out-file", txBody)
}
