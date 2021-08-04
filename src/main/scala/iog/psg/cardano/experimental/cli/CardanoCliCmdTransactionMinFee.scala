package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param.TxBodyFile
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdTransactionMinFee(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with TxBodyFile
    with CopyShim  {

  def protocolParamsFile(protocolParams: File): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--protocol-params-file", protocolParams))

  def txInCount(in: Int): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--tx-in-count", in.toString))

  def txOutCount(out: Int): CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--tx-out-count", out.toString))

  def witnessCount(witnessCount: Int):CardanoCliCmdTransactionMinFee =
    copy(builder.withParam("--witness-count", witnessCount.toString))

  def run(): String = stringValue()

  override type CONCRETECASECLASS = CardanoCliCmdTransactionMinFee
  override protected def copier = this
}
