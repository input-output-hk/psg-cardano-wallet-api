package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait TxBodyFile {
  self: CliCmd with CopyShim =>

  def txBodyFile(txBody: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--tx-body-file", txBody))
}
