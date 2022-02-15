package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, FileParam}

import java.io.File

trait SigningKeyFile {
  self: CliCmdBuilder =>

  def signingKeyFile(implicit fp: FileParam[SigningKeyFile]): Out = {
    signingKeyFile(fp.file)
  }

  def signingKeyFile(file: File): Out =
    build(_.withParam("--signing-key-file", file))
}
