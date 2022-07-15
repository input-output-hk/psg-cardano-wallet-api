package iog.psg.cardano.experimental.cli.api

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model._
import iog.psg.cardano.experimental.cli.processrunner.BlockingProcessRunner
import iog.psg.cardano.experimental.cli.processrunner.Ops._
import iog.psg.cardano.experimental.cli.util.{RandomTempFolder, Regexes}

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.ProcessBuilder
import scala.util.chaining.scalaUtilChainingOps


case class CardanoCliApi(cardanoCli: CardanoCli)(implicit networkChooser: NetworkChooser,
                                                 runner: BlockingProcessRunner,
                                                 ec: ExecutionContext,
                                                 val cliApiRootFolder: RandomTempFolder) {

  def protocolParams: CliApiRequest[ProtocolParams] = new CliApiRequest[ProtocolParams] {

    val outFile = ProtocolParams()

    override def execute: Future[ProtocolParams] = Future {

    val cmd = cardanoCli
      .query
        .protocolParameters
        .outFile(outFile.file)
        .withNetwork

      runner(cmd.processBuilder)

      outFile
    }


  }

  def hashKey[A <: KeyType](paymentVerKey: Key[A]): CliApiRequest[KeyHash[A]] = new CliApiRequest[KeyHash[A]] {

    val processBuilder: ProcessBuilder =
      cardanoCli
        .address
        .keyHash
        .paymentVerificationKeyFile(paymentVerKey.file)
        .processBuilder

    override def execute: Future[KeyHash[A]] = Future {
      val result = runner(processBuilder).asUnsafe[String]
      KeyHash(result)
    }

  }

  def generateKeyPair(): CliApiRequest[(Key[Verification], Key[Signing])] = new CliApiRequest[(Key[Verification], Key[Signing])]  {

    val verKey = Key[Verification]()
    val signKey = Key[Signing]()

    val processBuilder: ProcessBuilder =
      cardanoCli
        .address
        .keyGen
        .verificationKeyFile(verKey.file)
        .signingKeyFile(signKey.file)
        .normalKey
        .processBuilder

    override def execute: Future[(Key[Verification], Key[Signing])] = Future {
      runner(processBuilder).asUnsafe[Unit]
      (verKey, signKey)
    }

  }

  def genPaymentAddress(paymentVerificationKey: Key[Verification]): CliApiRequest[Address] = new CliApiRequest[Address] {

    override def execute: Future[Address] = Future {

      Address(runner(
        cardanoCli
          .address
          .build
          .paymentVerificationKeyFile(paymentVerificationKey.file)
          .withNetwork
          .processBuilder
      ).asUnsafe[String])
    }
  }

  def policyId(policy: Policy): CliApiRequest[PolicyId] = new CliApiRequest[PolicyId] {

    override def execute: Future[PolicyId] = Future {

      PolicyId(
        runner(cardanoCli
          .transaction
          .policid
          .scriptFile(policy.file)
          .processBuilder
        ).asUnsafe[String]
      )

    }

  }

  def utxo(address: Address): CliApiRequest[List[Utxo]] = new CliApiRequest[List[Utxo]] {

    override def execute: Future[List[Utxo]] = Future {

      val utxos = runner(

        cardanoCli
          .query
          .utxo
          .address(address.value)
          .withNetwork
          .processBuilder

      ).asUnsafe[List[String]]

      utxos
        .drop(2) // drop headers
        .map { line =>
          val data = Regexes.utxoPartSeparator.split(line)

          val nativeAssets: List[NativeAsset] = {
            data
              .tail
              .iterator
              .flatMap { asset =>
                val tokenAmountAndTokenInfo = Regexes.spaces.split(asset)

                for {
                  tokenAmount <- tokenAmountAndTokenInfo.headOption.flatMap(_.toIntOption)
                  policyIdAndName <- tokenAmountAndTokenInfo.lift(1).map(_.split('.'))
                  policyId <- policyIdAndName.headOption
                  tokenName <- policyIdAndName.lift(1).flatMap(Base16String.validate)
                } yield NativeAsset(AssetId(policyId, tokenName), tokenAmount)
              }
              .toList
          }

          val txHashTxIdLovelace: Array[String] = Regexes.spaces.split(data(0))

          Utxo(
            txHash = txHashTxIdLovelace(0),
            txIx = txHashTxIdLovelace(1).toInt,
            lovelace = txHashTxIdLovelace(2).toLong,
            assets = nativeAssets
          )
        }
    }
  }

  def buildTx(
    fee: Long,
    txIns: NonEmptyList[TxIn],
    txOuts: NonEmptyList[TxOut],
    maybeMetadata: Option[MetadataJson] = None,
    maybeMinting: Option[(NonEmptyList[NativeAsset], Policy)] = None,
    invalidBefore: Option[Long] = None,
    invalidHereafter: Option[Long] = None,
  ): CliApiRequest[Tx] = new CliApiRequest[Tx] {
    override def execute: Future[Tx] = Future {

      val tx = Tx()

      val (maybeNativeAssets, maybePolicy) = maybeMinting.unzip

      runner(cardanoCli
        .transaction
        .buildRaw
        .fee(fee)
        .txIns(txIns)
        .txOuts(txOuts)
        .optional(_.metadataJsonFile)(maybeMetadata.map(_.file))
        .optional[NonEmptyList[NativeAsset]](_.mint)(maybeNativeAssets)
        .optional(_.mintScriptFile)(maybePolicy.map(_.file))
        .optional(_.invalidBefore)(invalidBefore)
        .optional(_.invalidHereafter)(invalidHereafter)
        .outFile(tx.file)
        .processBuilder).asUnsafe[Unit]

      tx
    }
  }

  def calculateFee(
                    txBody: Tx,
                    protocolParams: ProtocolParams,
                    txInCount: Int,
                    txOutCount: Int,
                    witnessCount: Int): CliApiRequest[String] = new CliApiRequest[String] {

    override def execute: Future[String] = Future {
      runner(
        cardanoCli
          .transaction
          .calculateMinFee
          .txBodyFile(txBody.file)
          .txInCount(txInCount)
          .txOutCount(txOutCount)
          .witnessCount(witnessCount)
          .withNetwork
          .protocolParamsFile(protocolParams.file)
          .processBuilder
      ).asUnsafe[String]
    }
  }

  def signTx(
              keys: NonEmptyList[Key[_]],
              txBody: Tx
            ): CliApiRequest[SignedTx] = new CliApiRequest[SignedTx] {

    override def execute: Future[SignedTx] = Future {
      val signed = SignedTx()
      runner(
        cardanoCli
          .transaction
          .sign
          .signingKeyFiles(keys.map(_.file))
          .txBodyFile(txBody.file)
          .outFile(signed.file)
          .withNetwork
          .processBuilder
      ).asUnsafe[Unit]
      signed
    }
  }

  def txId(signedTx: SignedTx): CliApiRequest[String] = new CliApiRequest[String] {

    override def execute: Future[String] = Future {
      runner(
        cardanoCli
          .transaction
          .txId
          .txFile(signedTx.file)
          .processBuilder
      ).asUnsafe[String]
    }
  }

  def submitTx(signedTx: SignedTx): CliApiRequest[String] = new CliApiRequest[String] {

    override def execute: Future[String] = Future {
      runner(
        cardanoCli
          .transaction
          .submit
          .txFile(signedTx.file)
          .withNetwork
          .processBuilder
      ).asUnsafe[Unit]
    }.flatMap(_ => txId(signedTx).execute)
  }

}
