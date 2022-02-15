package iog.psg.cardano.experimental.nativeassets

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model.{Base16String, NativeAsset, NativeAssets, TxIn, TxOut, UTXO}
import iog.psg.cardano.experimental.cli.util.NetworkChooser

import java.io.File
import java.nio.file.{Files, Paths}

object NativeAssetsExample extends App {

  case class CardanoCliConfig(
    sudo: Boolean = false,
    testnetMagic: Option[Long] = None
  )

  implicit val networkChooser: NetworkChooser = ???

  import iog.psg.cardano.experimental.cli.implicits._

  def mint(
    tokenName: String,
    tokenAmount: Long,
    cardano: CardanoCli,
    paymentVerKey: File,
    paymentSignKey: File,
    paymentAddress: String,
    fileFactory: String => File
  ): Unit = {
    val tokenBase16 = Base16String(tokenName)

    val utxo: UTXO = cardano
      .utxo(paymentAddress)
      .maxByOption(_.lovelace)
      .filter(_.lovelace > 0)
      .getOrElse(throw new RuntimeException(s"please fund your address: $paymentAddress"))

    // for our transaction calculations, we need some of the current protocol parameters
    val protocolFile = fileFactory("protocol.json")
    cardano.protocolParams(protocolFile)

    // generate policy verification and signing keys
    val policyVerKey = fileFactory("policy.vkey")
    val policySignKey = fileFactory("policy.skey")

    cardano.genKeys(policyVerKey, policySignKey)

    val paymentVerKeyHash = cardano.hashKey(paymentVerKey)
    val policyVerKeyHash = cardano.hashKey(policyVerKey)

    val policy = Policy.All(
      NonEmptyList.of(
        Policy.Script(paymentVerKeyHash),
        Policy.Script(policyVerKeyHash),
      )
    )

    val policyScript = fileFactory("policy.script")
    policy.saveTo(policyScript)
    val policyId: String = cardano.policyId(policyScript)

    val mintTx = fileFactory("tx.raw")
    val nativeAssets = NativeAssets(policyId,  NonEmptyList.of(NativeAsset(tokenBase16, tokenAmount)))

    val txIns: NonEmptyList[TxIn] = NonEmptyList.of(
      TxIn(utxo.txHash, utxo.txIx)
    )

    val txOuts: NonEmptyList[TxOut] = NonEmptyList.of(
      TxOut(
        paymentAddress,
        utxo.lovelace,
        Some(nativeAssets)
      )
    )

    cardano.buildTx(
      fee = 0,
      txIns = txIns,
      txOuts = txOuts,
      maybeMinting = Some((nativeAssets, policyScript)),
      outFile = mintTx
    )

    // fee calculation
    val fee: Long = cardano.calculateFee(
      txBody = mintTx,
      protocolParams = protocolFile,
      txInCount = txIns.size,
      txOutCount = txOuts.size,
      witnessCount = policy.scripts.size
    )

    // re-build the transaction with real fee, ready to be signed
    val newTxOuts: NonEmptyList[TxOut] = NonEmptyList.of(
      TxOut(
        paymentAddress,
        utxo.lovelace - fee,
        Some(nativeAssets)
      )
    )

    cardano.buildTx(
      fee = fee,
      txIns = txIns,
      txOuts = newTxOuts,
      maybeMinting = Some((nativeAssets, policyScript)),
      outFile = mintTx
    )

    val signedTx = fileFactory("tx.signed")

    // sign the transaction
    cardano.signTx(
      keys = NonEmptyList.of(paymentSignKey, policySignKey),
      txBody = mintTx,
      outFile = signedTx
    )

    // submit the transaction
    cardano.submitTx(signedTx)
  }

  val cardano: CardanoCli = CardanoCli(Paths.get("./cardano-cli"))
    .withCardanoNodeSocketPath(Paths.get("/Users/roman/Library/Application Support/Daedalus Testnet/cardano-node.socket").toString)
    .withSudo(false)

  val wd = Files.createDirectories(Paths.get("test12345"))

  def fileFactory: String => File = wd.resolve(_).toFile

  // payment verification and signing keys are the first keys we need to create
  val paymentVerKey: File = fileFactory("payment.vkey")
  val paymentSignKey: File = fileFactory("payment.skey")

  val paymentAddr: String = cardano.genPaymentKeysAndAddress(paymentVerKey, paymentSignKey)

  println(s"Please fund this address: $paymentAddr")

  import scala.concurrent.duration._
  Thread.sleep(2.minutes.toMillis)

  mint(
    tokenName = "TestTokenPSG-4",
    tokenAmount = 160,
    cardano,
    paymentVerKey,
    paymentSignKey,
    paymentAddr,
    fileFactory
  )
}

