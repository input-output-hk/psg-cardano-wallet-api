package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait SigningKeyFile {
  self: CliCmdBuilder =>

  def signingKeyFile(file: File): Out =
    build(_.withParam("--signing-key-file", file))
}
