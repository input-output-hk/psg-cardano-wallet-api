package iog.psg.cardano.experimental.cli.command

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.model.{NativeAsset, TxIn, TxOut}
import iog.psg.cardano.experimental.cli.param.{CanRun, MaryEra, MetadataJsonFile, OutFile}
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

import java.io.File

case class CardanoCliCmdTransactionBuildRaw(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with OutFile
    with MaryEra
    with MetadataJsonFile
    with CanRun {

  def ttl(value: Long): CardanoCliCmdTransactionBuildRaw =
    withParam("--ttl", value)

  def fee(value: Long): CardanoCliCmdTransactionBuildRaw =
    withParam("--fee", value)

  def txIn(value: String): CardanoCliCmdTransactionBuildRaw =
    withParam("--tx-in", value)

  def txIn(in: TxIn): CardanoCliCmdTransactionBuildRaw =
    txIn(s"${in.txHash}#${in.txIx}")

  def txIns(values: NonEmptyList[TxIn]): CardanoCliCmdTransactionBuildRaw =
    values.foldLeft(this)(_.txIn(_))

  def txOut(value: String): CardanoCliCmdTransactionBuildRaw =
    withParam("--tx-out", value)

  def txOut(out: TxOut): CardanoCliCmdTransactionBuildRaw = {
    val addressOutput = s"${out.address}+${out.output}"
    val mintPart = NonEmptyList.fromList(out.assets).fold("")(x => "+" + mintParam(x))
    val finalParam = addressOutput + mintPart
    txOut(finalParam)
  }

  def txOuts(values: NonEmptyList[TxOut]): CardanoCliCmdTransactionBuildRaw =
    values.foldLeft(this)(_.txOut(_))

  def mint(value: String): CardanoCliCmdTransactionBuildRaw =
    withParam("--mint", value)

  def mint(assets: NonEmptyList[NativeAsset]): CardanoCliCmdTransactionBuildRaw =
    mint(mintParam(assets))

  def mintScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    withParam("--minting-script-file", file)

  def txinScriptFile(file: File): CardanoCliCmdTransactionBuildRaw =
    withParam("--txin-script-file", file)

  /**
   * Time that transaction is valid from (in slots)
   */
  def invalidBefore(slot: Long): CardanoCliCmdTransactionBuildRaw =
    withParam("--invalid-before", slot)

  /**
   * Time that transaction is valid until (in slots)
   */
  def invalidHereafter(slot: Long): CardanoCliCmdTransactionBuildRaw =
    withParam("--invalid-hereafter", slot)

  private def mintParam(assets: NonEmptyList[NativeAsset]): String = {
    assets.toList.iterator
      .map(a => s"${a.tokenAmount} ${a.assetId.policyId}.${a.assetId.name.value}")
      .mkString(" + ")
  }

  override type Out = CardanoCliCmdTransactionBuildRaw
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionBuildRaw = copy(b)
}
