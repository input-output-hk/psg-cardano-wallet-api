package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait TxFile {
  self: CliCmdBuilder =>

  def txFile(file: File): Out =
    withParam("--tx-file", file)
}
