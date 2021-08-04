package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait OutFile {
  self: CliCmd with CopyShim =>

  def outFile(txBody: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--out-file", txBody))
}
