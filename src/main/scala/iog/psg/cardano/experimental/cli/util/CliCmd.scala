package iog.psg.cardano.experimental.cli.util

trait CliCmd {

  protected val builder: ProcessBuilderHelper

  def stringRepr: String = builder.toCommand

  def exitValue(): Int = run[Int]

  def runOrFail(): Unit = run[Unit]

  protected def run[T: ProcessResult]: T = ProcessResult[T].apply(builder.processBuilder)

  protected def stringValue(): String = run[String]

  protected def allValues(): List[String] = run[List[String]]
}
