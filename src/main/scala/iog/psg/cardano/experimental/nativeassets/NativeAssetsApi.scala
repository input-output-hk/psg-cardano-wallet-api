package iog.psg.cardano.experimental.nativeassets

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model._
import iog.psg.cardano.experimental.cli.util.{CliSession, NetworkChooser, Regexes}

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters._

case class AssetBalance(id: AssetId, assetBalance: Long)

trait NativeAssetsApi {

  type PolicyId = String
  type TokenName = String
  type CardanoAddress = String
  type Problem = String
  type Result[T] = Either[Problem, T]

  def createNativeAsset(name: String, amount: Long): Result[AssetBalance]

  def transfer(assetBalance: AssetBalance, toAddress: CardanoAddress): Result[AssetBalance]

  def burn(assetId: AssetId, amount: Long): Result[AssetBalance]

  def balance(assetId: AssetId): Result[AssetBalance]

}

object NativeAssetsApi {

  def apply(
    cardano: CardanoCli,
    network: NetworkChooser,
    workingDir: Path,
    paymentAddress: String,
    paymentVerKey: File,
    paymentSignKey: File
  ): NativeAssetsApi = {

    new NativeAssetsApi {
      private implicit val cardanoCli: CardanoCli = cardano

      private case class AssetInfo(
        balance: Long,
        policyContent: String,
        policyKey: String
      )

      private val assetToInfo: scala.collection.concurrent.Map[AssetId, AssetInfo] =
        new java.util.concurrent.ConcurrentHashMap[AssetId, AssetInfo]().asScala

      import cats.implicits._
      import iog.psg.cardano.experimental.cli.implicits._

      private def subDir(path: Path): Path = Files.createDirectories(workingDir.resolve(path))

      override def createNativeAsset(name: String, amount: Long): Result[AssetBalance] = {
        val session = CliSession(
          workingFolder = subDir(Paths.get(s"native-asset-minting-$name-${System.currentTimeMillis()}")),
          network = network
        )

        import session._

        for {
          utxo <- getUTXOWithMaxFunds(paymentAddress)
          tokenBase16 = Base16String(name)

          _ <- session.getProtocolParams.run[Result[Unit]]
          _ <- session.genPolicyKeys.run[Result[Unit]]

          paymentVerKeyHash <- cardano.hashKey(paymentVerKey).run[Result[String]]
          policyVerKeyHash <- cardano.hashKey(policyVerKey.file).run[Result[String]]

          policy = Policy.All(
            NonEmptyList.of(
              Policy.Script(paymentVerKeyHash),
              Policy.Script(policyVerKeyHash),
            )
          )

          _ = policy.saveTo(policyScript.file)
          policyId <- cardano.policyId(policyScript.file).run[Result[String]]

          assetId = AssetId(policyId, tokenBase16)
          nativeAssets = NonEmptyList.of(NativeAsset(assetId, amount))
          txIn = TxIn(utxo.txHash, utxo.txIx)
          txOut = TxOut(paymentAddress, utxo.lovelace, utxo.assets ++ nativeAssets.toList)

          _ = cardanoCli.buildTx(
            fee = 0,
            txIns = NonEmptyList.one(txIn),
            txOuts = NonEmptyList.one(txOut),
            maybeMinting = Some((nativeAssets, policyScript.file)),
            outFile = txRaw.file
          ).run[Result[Unit]].getOrElse(throw new RuntimeException())

          fee <- cardanoCli.calculateFee(
            txBody = txRaw.file,
            protocolParams = protocolParams.file,
            txInCount = 1,
            txOutCount = 1,
            witnessCount = policy.scripts.size
          ).run[Result[String]].map {
            Regexes.spaces.split(_).apply(0).toLong
          }

          _ = cardanoCli.buildTx(
            fee = fee,
            txIns = NonEmptyList.one(txIn),
            txOuts = NonEmptyList.one(txOut.copy(output = txOut.output - fee)),
            maybeMinting = Some((nativeAssets, policyScript.file)),
            outFile = txRaw.file
          ).run[Result[Unit]].getOrElse(throw new RuntimeException())

          _ <- cardanoCli.signTx(
            keys = NonEmptyList.of(paymentSignKey, policySignKey.file),
            txBody = txRaw.file,
            outFile = txSigned.file
          ).run[Result[Unit]]

          _ <- cardanoCli.submitTx(txSigned.file).run[Result[Unit]]

          _ = assetToInfo.update(assetId,
            AssetInfo(
              balance = amount,
              policyContent = Files.readString(policyScript.file.toPath),
              policyKey = Files.readString(policySignKey.file.toPath)
            )
          )
        } yield {
          AssetBalance(
            id = assetId,
            assetBalance = amount
          )
        }
      }

      override def transfer(assetBalance: AssetBalance, toAddress: CardanoAddress): Result[AssetBalance] = {
        val assetId = assetBalance.id
        val amount = assetBalance.assetBalance

        val session = CliSession(
          workingFolder = subDir(Paths.get(s"native-asset-transfer-${assetId.name}-${System.currentTimeMillis()}")),
          network = network
        )

        import session._

        for {
          utxo <- cardano
            .utxo(paymentAddress)
            .find(_.assets.exists(_.assetId == assetId))
            .toRight("token not found")

          assetInfo <- assetToInfo
            .get(assetId)
            .filter(_.balance > amount)
            .toRight("insufficient token amount")

          asset = NativeAsset(assetId, assetInfo.balance)
          assetToTransfer = asset.copy(tokenAmount = amount)
          assetChange = asset.copy(tokenAmount = asset.tokenAmount - amount)

          remainingAssets = assetChange :: utxo.assets.filterNot(_.assetId == assetId)

          txIns = NonEmptyList.of(TxIn(utxo.txHash, utxo.txIx))

          lovelaceToTransfer = 1500000

          txOuts = NonEmptyList.of(
            TxOut(
              address = toAddress,
              output = lovelaceToTransfer,
              assets = assetToTransfer :: Nil
            ),
            TxOut(
              address = paymentAddress,
              output = utxo.lovelace,
              assets = remainingAssets
            )
          )

          _ <- session.getProtocolParams.run[Result[Unit]]

          _ <- cardano.buildTx(
            fee = 0,
            txIns = txIns,
            txOuts = txOuts,
            outFile = txRaw.file
          ).run[Result[Unit]]

          fee <- cardano.calculateFee(
            txBody = txRaw.file,
            protocolParams = protocolParams.file,
            txInCount = txIns.size,
            txOutCount = txOuts.size,
            witnessCount = 0
          ).run[Result[String]].map {
            Regexes.spaces.split(_).apply(0).toLong
          }

          newTxOuts = NonEmptyList.of(
            TxOut(
              address = toAddress,
              output = lovelaceToTransfer,
              assets = assetToTransfer :: Nil
            ),
            TxOut(
              address = paymentAddress,
              output = utxo.lovelace - fee - lovelaceToTransfer,
              assets = remainingAssets
            )
          )

          _ <- cardano.buildTx(
            fee = fee,
            txIns = txIns,
            txOuts = newTxOuts,
            outFile = txRaw.file
          ).run[Result[String]]

          _ <- cardano.signTx(
            keys = NonEmptyList.of(paymentSignKey),
            txBody = txRaw.file,
            outFile = txSigned.file,
          ).run[Result[String]]

          _ <- cardano.submitTx(txSigned.file).run[Result[String]]

        } yield AssetBalance(assetId, assetChange.tokenAmount)
      }

      override def burn(assetId: AssetId, amount: Long): Result[AssetBalance] = {
        val session = CliSession(
          workingFolder = subDir(Paths.get(s"native-asset-burning-${assetId.name}-${System.currentTimeMillis()}")),
          network = network
        )

        import session._

        for {
          utxo <- cardano
            .utxo(paymentAddress)
            .find(_.assets.exists(_.assetId == assetId))
            .toRight("token not found")

          remainingAssets = utxo.assets.filterNot(_.assetId == assetId)

          assetInfo <- assetToInfo
            .get(assetId)
            .filter(_.balance > amount)
            .toRight("insufficient token amount")

          asset = NativeAsset(assetId, assetInfo.balance)
          assetToBurn = asset.copy(tokenAmount = -amount)
          burnedAsset = asset.copy(tokenAmount = asset.tokenAmount - amount)

          txIns = NonEmptyList.of(TxIn(utxo.txHash, utxo.txIx))
          txOuts = NonEmptyList.of(TxOut(
            paymentAddress,
            utxo.lovelace,
            burnedAsset :: remainingAssets
          ))

          _ = Files.write(policyScript.file.toPath, assetInfo.policyContent.getBytes(StandardCharsets.UTF_8))

          minting = (NonEmptyList.of(assetToBurn), policyScript.file)

          _ <- cardanoCli.buildTx(
            fee = 0,
            txIns = txIns,
            txOuts = txOuts,
            maybeMinting = Some(minting),
            outFile = txRaw.file
          ).run[Result[Unit]]

          _ <- session.getProtocolParams.run[Result[Unit]]

          fee <- cardano
            .calculateFee(txRaw.file, protocolParams.file, txIns.size, txOuts.size, 2)
            .run[Result[String]]
            .map(Regexes.spaces.split(_).apply(0).toLong)

          newTxOuts = NonEmptyList.of(TxOut(
            paymentAddress,
            utxo.lovelace - fee,
            burnedAsset :: remainingAssets
          ))

          _ <- cardanoCli.buildTx(
            fee = fee,
            txIns = txIns,
            txOuts = newTxOuts,
            maybeMinting = Some(minting),
            outFile = txRaw.file
          ).run[Result[Unit]]

          _ = Files.write(policySignKey.file.toPath, assetInfo.policyKey.getBytes(StandardCharsets.UTF_8))

          _ <- cardanoCli.signTx(
            keys = NonEmptyList.of(paymentSignKey, policySignKey.file),
            txBody = txRaw.file,
            outFile = txSigned.file
          ).run[Result[Unit]]

          _ <- cardanoCli.submitTx(txSigned.file).run[Result[Unit]]

        } yield AssetBalance(assetId, burnedAsset.tokenAmount)
      }

      override def balance(assetId: AssetId): Result[AssetBalance] = {
        assetToInfo
          .get(assetId)
          .map(info => AssetBalance(assetId, info.balance))
          .toRight("asset not found")
      }

      private def getUTXOWithMaxFunds(address: String)(implicit network: NetworkChooser): Either[String, UTXO] = {
        cardano
          .utxo(address)
          .maxByOption(_.lovelace)
          .filter(_.lovelace > 0)
          .toRight(s"Insufficient funds, address: $paymentAddress")
      }
    }
  }
}
