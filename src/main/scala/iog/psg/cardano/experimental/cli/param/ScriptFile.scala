package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait ScriptFile {
  self: CliCmdBuilder =>

  def scriptFile(file: File): Out =
    withParam("--script-file", file)
}
