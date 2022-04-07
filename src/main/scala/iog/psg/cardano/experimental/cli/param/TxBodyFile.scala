package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait TxBodyFile {
  self: CliCmdBuilder =>

  def txBodyFile(txBody: File): Out =
    withParam("--tx-body-file", txBody)
}
