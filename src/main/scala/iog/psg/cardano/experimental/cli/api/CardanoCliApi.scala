package iog.psg.cardano.experimental.cli.api

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model._
import iog.psg.cardano.experimental.cli.param.MetadataJsonFile
import iog.psg.cardano.experimental.cli.util.{RandomTempFolder, Regexes}

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.ProcessBuilder
import scala.util.chaining.scalaUtilChainingOps


case class CardanoCliApi(cardanoCli: CardanoCli)(implicit networkChooser: NetworkChooser,
                                                 runner: ProcessBuilderRunner,
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

      runner.runUnit(cmd.processBuilder)

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
      val result = runner.runString(processBuilder)
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
      runner.runUnit(processBuilder)
      (verKey, signKey)
    }

  }

  def genPaymentAddress(paymentVerificationKey: Key[Verification]): CliApiRequest[Address] = new CliApiRequest[Address] {

    override def execute: Future[Address] = Future {

      Address(runner.runString(
        cardanoCli
          .address
          .build
          .paymentVerificationKeyFile(paymentVerificationKey.file)
          .withNetwork
          .processBuilder
      ))
    }
  }

  def policyId(policy: Policy): CliApiRequest[PolicyId] = new CliApiRequest[PolicyId] {

    override def execute: Future[PolicyId] = Future {

      PolicyId(
        runner.runString(cardanoCli
          .transaction
          .policid
          .scriptFile(policy.file)
          .processBuilder
        )
      )

    }

  }

  def utxo(address: Address): CliApiRequest[List[Utxo]] = new CliApiRequest[List[Utxo]] {

    override def execute: Future[List[Utxo]] = Future {

      val utxos = runner.runListString(

        cardanoCli
          .query
          .utxo
          .address(address.value)
          .withNetwork
          .processBuilder

      )

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
             ): CliApiRequest[Tx] = new CliApiRequest[Tx] {
    override def execute: Future[Tx] = Future {

      val tx = Tx()

      runner.runUnit(cardanoCli
        .transaction
        .buildRaw
        .fee(fee)
        .pipe(builder => maybeMetadata.fold(builder){ meta =>
          builder.metadataJsonFile(meta.file)
        })
        .pipe(txIns.foldLeft(_)(_.txIn(_)))
        .pipe(txOuts.foldLeft(_)(_.txOut(_)))
        .pipe(builder => maybeMinting.fold(builder) {
          case (assets, mintScriptFile) =>
            builder
              .mint(assets)
              .mintScriptFile(mintScriptFile.file)
        })
        .outFile(tx.file)
        .processBuilder)

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
      runner.runString(
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
      )
    }
  }

  def signTx(
              keys: NonEmptyList[Key[_]],
              txBody: Tx
            ): CliApiRequest[SignedTx] = new CliApiRequest[SignedTx] {

    override def execute: Future[SignedTx] = Future {
      val signed = SignedTx()
      runner.runUnit(
        cardanoCli
          .transaction
          .sign
          .pipe(keys.map(_.file).foldLeft(_)(_.signingKeyFile(_)))
          .txBodyFile(txBody.file)
          .outFile(signed.file)
          .withNetwork
          .processBuilder
      )
      signed
    }
  }

  def submitTx(signedTx: SignedTx): CliApiRequest[Unit] = new CliApiRequest[Unit] {

    override def execute: Future[Unit] = Future {
      runner.runUnit(
        cardanoCli
          .transaction
          .submit
          .txFile(signedTx.file)
          .withNetwork
          .processBuilder
      )
    }
  }

}
