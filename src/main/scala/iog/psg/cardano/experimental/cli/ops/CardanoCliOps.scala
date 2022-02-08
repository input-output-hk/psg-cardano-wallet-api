package iog.psg.cardano.experimental.cli.ops

import iog.psg.cardano.experimental.cli.command.CardanoCli

import java.io.File

final class CardanoCliOps(private val cardanoCli: CardanoCli) extends AnyVal {

  def keyHash(paymentVerKeyFile: File): String = {
    cardanoCli
      .address
      .keyHash
      .paymentVerificationKeyFile(paymentVerKeyFile)
      .res()
  }

  def keyGen(
    verificationKeyFile: File,
    signingKeyFile: File,
  ): Unit = {
    cardanoCli
      .address
      .keyGen
      .verificationKeyFile(verificationKeyFile)
      .signingKeyFile(signingKeyFile)
      .normalKey
      .runOrFail()
  }

  def policyId(policyScriptFile: File): String = {
    cardanoCli
      .transaction
      .policid
      .scriptFile(policyScriptFile)
      .res()
  }
}

trait CardanoCliSyntax {

  implicit def toCardanoCliOps(cardanoCli: CardanoCli): CardanoCliOps =
    new CardanoCliOps(cardanoCli)
}
