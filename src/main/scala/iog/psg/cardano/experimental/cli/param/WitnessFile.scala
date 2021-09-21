package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait WitnessFile {
  self: CliCmd with CopyShim =>

  def witnessFile(txBody: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--witness-file", txBody))
}
