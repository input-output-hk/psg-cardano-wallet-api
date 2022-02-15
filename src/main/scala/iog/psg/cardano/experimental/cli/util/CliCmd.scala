package iog.psg.cardano.experimental.cli.util

import iog.psg.cardano.experimental.cli.param.TestnetMagic

trait CliCmd {

  protected val builder: ProcessBuilderHelper

  def stringRepr(implicit net: NetworkChooser): String = addNetwork.toCommand

  def exitValue(implicit net: NetworkChooser): Int = run[Int]

  def runOrFail(implicit net: NetworkChooser): Unit = run[Unit]

  def run[T: ProcessResult](implicit net: NetworkChooser): T = {

    ProcessResult[T]
      .apply(
        addNetwork
          .processBuilder
      )
  }

  private def addNetwork(implicit net: NetworkChooser): ProcessBuilderHelper = {

    this match {
      case _ : TestnetMagic =>
        net.withNetwork(builder)

      case _ => builder
    }

  }
  protected def stringValue(implicit net: NetworkChooser): String = run[String]

  protected def allValues(implicit net: NetworkChooser): List[String] = run[List[String]]
}
