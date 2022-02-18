package iog.psg.cardano.experimental.cli

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model.{Base16String, NativeAsset, TxIn, TxOut, UTXO}
import iog.psg.cardano.experimental.cli.util.{CliSession, NetworkChooser, Regexes}
import iog.psg.cardano.experimental.nativeassets.Policy

import java.io.File
import java.nio.file.{Files, Path, Paths}

trait NativeAssetsApi {

  type PolicyId = String
  type TokenName = String
  type CardanoAddress = String
  type Problem = String
  type Result[T] = Either[Problem, T]

  case class AssetId(policyId: PolicyId, name: TokenName)
  case class AssetBalance(id: AssetId, assetBalance: Long)

  def createNativeAsset(name: String, initialAmount: Long): Result[AssetBalance]

  def transfer(assetBalance: AssetBalance, toAddress: CardanoAddress): Result[AssetBalance]

  def burn(assetId: AssetId): Result[AssetBalance]

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

      import iog.psg.cardano.experimental.cli.implicits._
      import cats.implicits._

      private def subDir(path: Path): Path = Files.createDirectories(workingDir.resolve(path))

      override def createNativeAsset(
        name: String,
        initialAmount: Long
      ): Result[AssetBalance] = {

        val session = CliSession(
          workingFolder = subDir(Paths.get(s"native-asset-minting-$name-${System.currentTimeMillis()}")),
          network = network
        )

        import session._

        def getUTXO(address: String): Either[String, UTXO] = {
          cardano
            .utxo(address)
            .maxByOption(_.lovelace)
            .filter(_.lovelace > 0)
            .toRight(s"Insufficient funds, address: $paymentAddress")
        }

        for {
          utxo <- getUTXO(paymentAddress)
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

          nativeAssets = NonEmptyList.of(NativeAsset(tokenBase16, initialAmount, policyId))
          txIn = TxIn(utxo.txHash, utxo.txIx)
          txOut = TxOut(paymentAddress, utxo.lovelace, nativeAssets.toList)

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
            Regexes.spaces.split(_)
              .apply(0)
              .toLong
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

        } yield {
          AssetBalance(
            id = AssetId(policyId, name),
            assetBalance = initialAmount
          )
        }
      }

      override def transfer(assetBalance: AssetBalance, toAddress: CardanoAddress): Result[AssetBalance] = {
        ???
      }

      override def burn(assetId: AssetId): Result[AssetBalance] = {
        ???
      }

      override def balance(assetId: AssetId): Result[AssetBalance] = {
        ???
      }
    }
  }
}

object Test extends App {
  import iog.psg.cardano.experimental.cli.implicits._

  val cardano = CardanoCli(Paths.get("/Users/roman/Downloads/cardano-node-1.32.1-macos (2)/cardano-cli"))
    .withCardanoNodeSocketPath(Paths.get("/Users/roman/Library/Application Support/Daedalus Testnet/cardano-node.socket").toString)
    .withSudo(false)

  implicit val network: NetworkChooser = NetworkChooser.DefaultTestnet

  val workingDirectory = Paths.get("native-assets-api-test")
  val paymentVerKey = workingDirectory.resolve("payment.vkey").toFile
  val paymentSignKey = workingDirectory.resolve("payment.skey").toFile

  val paymentAddress = "addr_test1vrxl2yr3nz545j8yej0c0jcqxhkq3t27hxaz9ry8lfdezwqf6zchv"
    // cardano.genPaymentKeysAndAddress(paymentVerKey, paymentSignKey)

  val nativeAssetsApi = NativeAssetsApi.apply(
    cardano = cardano,
    network = network,
    workingDir = workingDirectory,
    paymentAddress = paymentAddress,
    paymentVerKey = paymentVerKey,
    paymentSignKey = paymentSignKey,
  )

  val createdAsset = nativeAssetsApi.createNativeAsset(
    name = "native-asset-test-v1",
    initialAmount = 150
  )

  println(createdAsset)
}