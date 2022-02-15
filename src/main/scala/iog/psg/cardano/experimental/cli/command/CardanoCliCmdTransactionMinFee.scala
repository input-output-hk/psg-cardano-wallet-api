package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.{TestnetMagic, TxBodyFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, NetworkChooser, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdTransactionMinFee(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with TxBodyFile
    with TestnetMagic {

  def protocolParamsFile(protocolParams: File): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--protocol-params-file", protocolParams))

  def txInCount(in: Int): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--tx-in-count", in))

  def txOutCount(out: Int): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--tx-out-count", out))

  def witnessCount(witnessCount: Int): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--witness-count", witnessCount))

  def res(implicit net: NetworkChooser): String = run[String]

  override type Out = CardanoCliCmdTransactionMinFee
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionMinFee = copy(b)
}
