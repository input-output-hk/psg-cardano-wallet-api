package iog.psg.cardano.experimental.cli.processrunner

import scala.sys.process.{ProcessLogger => SysProcessLogger}

private class ProcessLogger extends SysProcessLogger {

  private var outBuffer: List[String] = Nil
  private var errBuffer: List[String] = Nil

  override def out(s: => String): Unit = outBuffer = outBuffer :+ s

  override def err(s: => String): Unit = errBuffer = errBuffer :+ s

  override def buffer[T](f: => T): T = f

  def hasError: Boolean = errBuffer.nonEmpty

  def result: List[String] = outBuffer

  def error: Option[List[String]] = Option.when(errBuffer.nonEmpty)(errBuffer)
}

