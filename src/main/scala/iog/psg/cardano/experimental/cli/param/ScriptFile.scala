package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait ScriptFile {
  self: CliCmd with CopyShim =>

  def scriptFile(scriptFile: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--script-file", scriptFile))

}
