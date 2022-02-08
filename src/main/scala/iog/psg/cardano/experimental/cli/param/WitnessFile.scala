package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait WitnessFile {
  self: CliCmdBuilder =>

  def witnessFile(txBody: File): Out =
    build(_.withParam("--witness-file", txBody))
}
