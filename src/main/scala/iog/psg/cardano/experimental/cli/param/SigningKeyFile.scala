package iog.psg.cardano.experimental.cli.param

import cats.Foldable

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait SigningKeyFile {
  this: CliCmdBuilder =>

  private val paramName: String = "--signing-key-file"

  def signingKeyFile(file: File): Out =
    withParam(paramName, file)

  def signingKeyFiles[C[_]: Foldable](files: C[File]): Out =
    withParams(paramName, files)
}
