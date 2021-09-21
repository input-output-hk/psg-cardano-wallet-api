package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

import java.io.File

trait SigningKeyFile {
  self: CliCmd with CopyShim =>

  def signingKeyFile(scriptFile: File): CONCRETECASECLASS =
    copier.copy(builder.withParam("--signing-key-file", scriptFile))
}
