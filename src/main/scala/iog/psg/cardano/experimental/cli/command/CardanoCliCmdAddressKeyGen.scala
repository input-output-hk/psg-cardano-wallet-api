package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.SigningKeyFile
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdAddressKeyGen(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with SigningKeyFile {

  lazy val normalKey: CardanoCliCmdAddressKeyGenNormalKey =
    CardanoCliCmdAddressKeyGenNormalKey(builder.withParam("--normal-key"))

  def verificationKeyFile(verificationKeyFile: File): CardanoCliCmdAddressKeyGen =
    CardanoCliCmdAddressKeyGen(builder.withParam("--verification-key-file", verificationKeyFile))

  override type Out = CardanoCliCmdAddressKeyGen

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdAddressKeyGen = copy(b)
}
