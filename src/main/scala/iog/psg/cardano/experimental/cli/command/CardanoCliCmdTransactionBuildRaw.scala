package iog.psg.cardano.experimental.cli.command

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.model.{NativeAsset, NativeAssets, TxIn, TxOut}
import iog.psg.cardano.experimental.cli.param.{MaryEra, OutFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdTransactionBuildRaw(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with OutFile
    with MaryEra {

  def ttl(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--ttl", value.toString))

  def fee(value: Long): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--fee", value.toString))

  def txIn(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-in", value))

  def txIn(in: TxIn): CardanoCliCmdTransactionBuildRaw =
    txIn(s"${in.txHash}#${in.txIx}")

  def txOut(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--tx-out", value))

  def txOut(out: TxOut): CardanoCliCmdTransactionBuildRaw = {
    val addressOutput = s"${out.address}+${out.output}"
    val finalParam = out.assets.fold(addressOutput)(ax => addressOutput + s"+${mintParam(ax.policyId, ax.assets)}")
    txOut(finalParam)
  }

  def mint(value: String): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--mint", value))

  def mint(assets: NativeAssets): CardanoCliCmdTransactionBuildRaw = {
    mint(mintParam(assets.policyId, assets.assets))
  }

  def mintScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--minting-script-file", file))

  def txinScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    copy(builder.withParam("--txin-script-file", file))

  def run(): Int = exitValue()

  private def mintParam(policyId: String, assets: NonEmptyList[NativeAsset]): String = {
    assets
      .iterator
      .map(a => s"${a.tokenAmount} $policyId.${a.tokenName.value}")
      .mkString(" + ")
  }

  override type Out = CardanoCliCmdTransactionBuildRaw
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionBuildRaw = copy(b)
}
