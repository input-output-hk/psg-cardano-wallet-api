package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdAddressKeyHash(protected val builder: ProcessBuilderHelper) extends CliCmd {
  def paymentVerificationString(bech32EncodedKey: String): CardanoCliCmdAddressKeyHashString =
    CardanoCliCmdAddressKeyHashString(builder.withParam("--payment-verification-key", "'" + bech32EncodedKey + "'"))

  def paymentVerificationFile(pathToBech32EncodedKey: File): CardanoCliCmdAddressKeyHashFile =
    CardanoCliCmdAddressKeyHashFile(builder.withParam("--payment-verification-key-file", pathToBech32EncodedKey))
}