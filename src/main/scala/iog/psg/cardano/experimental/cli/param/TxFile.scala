package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait TxFile {
  self: CliCmd with CopyShim =>

  def txFile(txFile: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--tx-file", txFile))
}
