package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdAddressBuildScript(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with TestnetMagic
    with CopyShim
    with OutFile {

  override type CONCRETECASECLASS = CardanoCliCmdAddressBuildScript
  val copier = this

  def withPaymentScriptFile(file: File): CardanoCliCmdAddressBuildScript = {
    copy(builder.withParam("--payment-script-file", file))
  }

  def run(): Int = exitValue()
  def string(): String = stringValue()
}