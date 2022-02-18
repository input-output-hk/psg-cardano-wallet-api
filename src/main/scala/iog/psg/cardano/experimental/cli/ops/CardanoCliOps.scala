package iog.psg.cardano.experimental.cli.ops

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.{CardanoCli, CardanoCliCmdAddressBuild, CardanoCliCmdAddressKeyGenNormalKey, CardanoCliCmdAddressKeyHash, CardanoCliCmdQueryProtocol, CardanoCliCmdTransactionBuildRaw, CardanoCliCmdTransactionMinFee, CardanoCliCmdTransactionPolicyId, CardanoCliCmdTransactionSign, CardanoCliCmdTransactionSubmit}
import iog.psg.cardano.experimental.cli.model.{Base16String, NativeAsset, TxIn, TxOut, UTXO}
import iog.psg.cardano.experimental.cli.util.{NetworkChooser, Regexes}

import scala.util.chaining._
import java.io.File

final class CardanoCliOps(private val cardanoCli: CardanoCli) {

  def hashKey(paymentVerKeyFile: File): CardanoCliCmdAddressKeyHash = {
    cardanoCli
      .address
      .keyHash
      .paymentVerificationKeyFile(paymentVerKeyFile)
  }

  def genKeys(verKey: File, signKey: File): CardanoCliCmdAddressKeyGenNormalKey = {
    cardanoCli
      .address
      .keyGen
      .verificationKeyFile(verKey)
      .signingKeyFile(signKey)
      .normalKey
  }

  def genPaymentAddress(verKey: File): CardanoCliCmdAddressBuild = {
    cardanoCli
      .address
      .build
      .paymentVerificationKeyFile(verKey)
  }

  def genPaymentKeysAndAddress(verKey: File, signKey: File)(implicit net: NetworkChooser): String = {
    genKeys(verKey, signKey).run[Unit]
    genPaymentAddress(verKey).run[String]
  }

  def policyId(policyScriptFile: File): CardanoCliCmdTransactionPolicyId = {
    cardanoCli
      .transaction
      .policid
      .scriptFile(policyScriptFile)
  }

  def utxo(address: String)(implicit net: NetworkChooser): List[UTXO] = {
    cardanoCli
      .query
      .utxo
      .address(address)
      .run[List[String]]
      .drop(2) // drop headers
      .map { line =>
        val data = Regexes.spaces.split(line)

        def get(ix: Int): Option[String] = data.lift(ix)

        val maybeAssets = for {
          tokenAmount <- get(5).flatMap(_.toIntOption)
          policyIdAndName <- get(6).map(_.split('.'))
          policyId <- policyIdAndName.lift(0)
          tokenName <- policyIdAndName.lift(1).flatMap(Base16String.validate)
        } yield NativeAsset(tokenName, tokenAmount, policyId)

        UTXO(data(0), data(1).toInt, data(2).toLong, maybeAssets.toList)
      }
  }

  def protocolParams(outFile: File): CardanoCliCmdQueryProtocol = {
    cardanoCli
      .query
      .protocolParameters
      .outFile(outFile)
  }

  def buildTx(
    fee: Long,
    txIns: NonEmptyList[TxIn],
    txOuts: NonEmptyList[TxOut],
    maybeMinting: Option[(NonEmptyList[NativeAsset], File)] = None,
    outFile: File
  ): CardanoCliCmdTransactionBuildRaw = {
    cardanoCli
      .transaction
      .buildRaw
      .fee(fee)
      .pipe(txIns.foldLeft(_)(_.txIn(_)))
      .pipe(txOuts.foldLeft(_)(_.txOut(_)))
      .pipe(builder => maybeMinting.fold(builder) {
        case (assets, mintScriptFile) =>
          builder
            .mint(assets)
            .mintScriptFile(mintScriptFile)
      })
      .outFile(outFile)
  }

  def calculateFee(
    txBody: File,
    protocolParams: File,
    txInCount: Int,
    txOutCount: Int,
    witnessCount: Int,
  ): CardanoCliCmdTransactionMinFee = {
    cardanoCli
      .transaction
      .calculateMinFee
      .txBodyFile(txBody)
      .txInCount(txInCount)
      .txOutCount(txOutCount)
      .witnessCount(witnessCount)
      .protocolParamsFile(protocolParams)
  }

  def signTx(
    keys: NonEmptyList[File],
    txBody: File,
    outFile: File
  ): CardanoCliCmdTransactionSign = {
    cardanoCli
      .transaction
      .sign
      .pipe(keys.foldLeft(_)(_.signingKeyFile(_)))
      .txBodyFile(txBody)
      .outFile(outFile)
  }

  def submitTx(signedTx: File): CardanoCliCmdTransactionSubmit = {
    cardanoCli
      .transaction
      .submit
      .txFile(signedTx)
  }
}

trait CardanoCliSyntax {

  implicit def toCardanoCliOps(cardanoCli: CardanoCli): CardanoCliOps =
    new CardanoCliOps(cardanoCli)
}
