package iog.psg.cardano.experimental.cli.ops

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model.{NativeAssets, TxIn, TxOut, UTXO}
import iog.psg.cardano.experimental.cli.util.Regexes

import scala.util.chaining._
import java.io.File

final class CardanoCliOps(private val cardanoCli: CardanoCli) extends AnyVal {

  def hashKey(paymentVerKeyFile: File): String = {
    cardanoCli
      .address
      .keyHash
      .paymentVerificationKeyFile(paymentVerKeyFile)
      .res()
  }

  def genKeys(verKey: File, signKey: File): Unit = {
    cardanoCli
      .address
      .keyGen
      .verificationKeyFile(verKey)
      .signingKeyFile(signKey)
      .normalKey
      .runOrFail()
  }

  def genPaymentAddress(verKey: File, testnet: Boolean): String = {
    cardanoCli
      .address
      .build
      .paymentVerificationKeyFile(verKey)
      .pipe(c => if (testnet) c.testnetMagic else c)
      .res()
  }

  def genPaymentKeysAndAddress(verKey: File, signKey: File, testnet: Boolean): String = {
    genKeys(verKey, signKey)
    genPaymentAddress(verKey, testnet)
  }

  def policyId(policyScriptFile: File): String = {
    cardanoCli
      .transaction
      .policid
      .scriptFile(policyScriptFile)
      .res()
  }

  def utxo(address: String, testnet: Boolean): List[UTXO] = {
    cardanoCli
      .query
      .utxo
      .address(address)
      .pipe(c => if (testnet) c.testnetMagic else c)
      .run[List[String]]
      .drop(2) // drop headers
      .map { line =>
        val data = Regexes.spaces.split(line)
        UTXO(data(0), data(1).toInt, data(2).toLong)
      }
  }

  def protocolParams(outFile: File, testnet: Boolean): Unit = {
    cardanoCli
      .query
      .protocolParameters
      .pipe(c => if (testnet) c.testnetMagic else c)
      .outFile(outFile)
      .runOrFail()
  }

  def buildTx(
    fee: Long,
    txIns: NonEmptyList[TxIn],
    txOuts: NonEmptyList[TxOut],
    maybeMinting: Option[(NativeAssets, File)],
    outFile: File
  ): Unit = {
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
      .runOrFail()
  }

  def calculateFee(
    txBody: File,
    protocolParams: File,
    txInCount: Int,
    txOutCount: Int,
    witnessCount: Int,
    testnet: Boolean
  ): Long = {
    cardanoCli
      .transaction
      .calculateMinFee
      .txBodyFile(txBody)
      .txInCount(txInCount)
      .txOutCount(txOutCount)
      .witnessCount(witnessCount)
      .pipe(c => if (testnet) c.testnetMagic else c)
      .protocolParamsFile(protocolParams)
      .res()
      .pipe(Regexes.spaces.split(_))
      .apply(0)
      .toLong
  }

  def signTx(
    keys: NonEmptyList[File],
    txBody: File,
    outFile: File,
    testnet: Boolean
  ): Unit = {
    cardanoCli
      .transaction
      .sign
      .pipe(keys.foldLeft(_)(_.signingKeyFile(_)))
      .pipe(c => if (testnet) c.testnetMagic else c)
      .txBodyFile(txBody)
      .outFile(outFile)
      .runOrFail()
  }

  def submitTx(
    signedTx: File,
    testnet: Boolean
  ): Unit = {
    cardanoCli
      .transaction
      .submit
      .txFile(signedTx)
      .pipe(c => if (testnet) c.testnetMagic else c)
      .runOrFail()
  }
}

trait CardanoCliSyntax {

  implicit def toCardanoCliOps(cardanoCli: CardanoCli): CardanoCliOps =
    new CardanoCliOps(cardanoCli)
}
