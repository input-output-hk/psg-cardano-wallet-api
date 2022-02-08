package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.OutFile
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdTransactionBuildRaw(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with OutFile {

  def ttl(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--ttl", value.toString))

  def fee(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--fee", value.toString))

  def txIn(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-in", value))

  def txOut(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-out", value))

  def mint(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--mint", value))

  def mintScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--minting-script-file", file))

  def txinScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--txin-script-file", file))

  def run(): Int = exitValue()

  override type Out = CardanoCliCmdTransactionBuildRaw

  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionBuildRaw = copy(b)
}
