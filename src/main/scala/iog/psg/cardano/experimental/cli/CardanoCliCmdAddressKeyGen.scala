package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdAddressKeyGen(protected val builder: ProcessBuilderHelper) extends CliCmd {
  lazy val normalKey: CardanoCliCmdAddressKeyGenNormalKey =
    CardanoCliCmdAddressKeyGenNormalKey(builder.withParam("--normal-key"))

  def verificationKeyFile(verificationKeyFile: File): CardanoCliCmdAddressKeyGen = {
    CardanoCliCmdAddressKeyGen(
      builder.withParam("--verification-key-file", verificationKeyFile)
    )
  }

  def signingKeyFile(signingKeyFile: File): CardanoCliCmdAddressKeyGen = {
    CardanoCliCmdAddressKeyGen(
      builder.withParam("--signing-key-file", signingKeyFile)
    )
  }
}