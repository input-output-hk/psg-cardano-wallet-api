package iog.psg.cardano.experimental.cli

import java.io.File
import java.nio.file.Files

object FileUtils {

  implicit final class IsEmpty(private val file: File) extends AnyVal {

    def isEmpty: Boolean = Files.size(file.toPath) == 0
    def nonEmpty: Boolean = !isEmpty
  }
}
