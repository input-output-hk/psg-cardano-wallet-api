package iog.psg.cardano.experimental.cli.processrunner

import scala.sys.process.ProcessBuilder


trait BlockingProcessRunner {
  def apply(process: ProcessBuilder): BlockingProcessResult
}

object BlockingProcessRunner extends BlockingProcessRunner {
  def apply(process: ProcessBuilder): BlockingProcessResult = {
    val logger = new ProcessLogger
    val p      = process.run(logger)
    val i      = p.exitValue()
    BlockingProcessResult(i, logger.result, logger.error)
  }
}

