package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{OutFile, SigningKeyFile, TestnetMagic, TxBodyFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionSign(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TestnetMagic
    with SigningKeyFile
    with TxBodyFile
    with OutFile {

  override type Out = CardanoCliCmdTransactionSign

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionSign = copy(b)
}
