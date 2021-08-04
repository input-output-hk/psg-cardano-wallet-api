package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param._
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdAddressBuild(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with TestnetMagic
    with CopyShim
    with OutFile {

  override type CONCRETECASECLASS = CardanoCliCmdAddressBuild
  val copier = this

  def paymentVerificationKey(verificationKey: String): CardanoCliCmdAddressBuild =
    CardanoCliCmdAddressBuild(builder.withParam("--payment-verification-key", verificationKey))

  def paymentVerificationKeyFile(verificationKeyFile: File): CardanoCliCmdAddressBuild =
    CardanoCliCmdAddressBuild(builder.withParam("--payment-verification-key-file", verificationKeyFile))

  def paymentScriptFile(paymentScriptFile: File): CardanoCliCmdAddressBuild =
    CardanoCliCmdAddressBuild(builder.withParam("--payment-script-file", paymentScriptFile))

  def run(): Int = exitValue()
}