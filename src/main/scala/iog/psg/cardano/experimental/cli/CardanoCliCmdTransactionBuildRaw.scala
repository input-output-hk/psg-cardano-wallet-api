package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.param.OutFile
import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdTransactionBuildRaw(protected val builder: ProcessBuilderHelper)
  extends CliCmd
    with OutFile
    with CopyShim {

  def ttl(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--ttl", value.toString))

  def fee(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--fee", value.toString))

  def txIn(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-in", value))

  def txOut(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-out", value))

  def run(): Int = exitValue()

  override type CONCRETECASECLASS = CardanoCliCmdTransactionBuildRaw
  override protected def copier = this
}