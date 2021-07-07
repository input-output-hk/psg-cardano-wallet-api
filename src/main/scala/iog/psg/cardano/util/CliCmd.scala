package iog.psg.cardano.util

import java.io.File
import scala.collection.mutable
import scala.sys.process.ProcessLogger


trait CliCmd {

  protected val builder: ProcessBuilderHelper

  protected def requireFile(f: File): File = {
    require(f.exists() && f.isFile,
      s"$f must exist and be a file")
    f
  }

  protected def exitValue(): Int = {
    builder
      .processBuilder
      .run()
      .exitValue()
  }

  protected def stringValue(): String = {
    builder
      .processBuilder
      .lazyLines.head
  }

  protected def allValues(): Seq[String] = {
    val buf = mutable.ArrayBuffer.empty[String]
    val result: String = builder
      .processBuilder
      .!!<(ProcessLogger(s => buf.addOne(s)))
    result +: buf.toSeq
  }


}

