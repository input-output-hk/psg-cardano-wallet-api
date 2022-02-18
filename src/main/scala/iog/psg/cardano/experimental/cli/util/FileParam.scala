package iog.psg.cardano.experimental.cli.util

import java.io.File

trait FileParam[T] {
  def file: File
}
