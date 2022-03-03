package iog.psg.cardano.experimental.cli.api

import iog.psg.cardano.experimental.cli.util.ProcessResult

import scala.sys.process.ProcessBuilder

object DefaultProcessBuilderRunner extends ProcessBuilderRunner {

  override def runString(processBuilder: ProcessBuilder): String =
    ProcessResult[String].apply(processBuilder)

  override def runUnit(processBuilder: ProcessBuilder): Unit =
    ProcessResult[Unit].apply(processBuilder)

  override def runListString(processBuilder: ProcessBuilder): List[String] =
    ProcessResult[List[String]].apply(processBuilder)
}
