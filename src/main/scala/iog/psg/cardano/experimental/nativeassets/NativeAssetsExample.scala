package iog.psg.cardano.experimental.nativeassets

import cats.Monoid
import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model.{Base16String, NativeAsset, TxIn, TxOut, UTXO}

import java.io.File
import java.nio.file.{Files, Paths}

object NativeAssetsExample extends App {

  case class CardanoCliConfig(
    sudo: Boolean = false,
    testnetMagic: Option[Long] = None
  )

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
      .utxo(paymentAddress, testnet = true)
      .maxByOption(_.lovelace)
      .filter(_.lovelace > 0)
      .getOrElse(throw new RuntimeException(s"please fund your address: $paymentAddress"))

    // for our transaction calculations, we need some of the current protocol parameters
    val protocolFile = fileFactory("protocol.json")
    cardano.protocolParams(protocolFile, testnet = true)

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
    val nativeAssets = NonEmptyList.of(NativeAsset(tokenBase16, tokenAmount, policyId))

    val txIns: NonEmptyList[TxIn] = NonEmptyList.of(
      TxIn(utxo.txHash, utxo.txIx)
    )

    val txOuts: NonEmptyList[TxOut] = NonEmptyList.of(
      TxOut(
        paymentAddress,
        utxo.lovelace,
        nativeAssets.toList
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
      witnessCount = policy.scripts.size,
      testnet = true
    )

    // re-build the transaction with real fee, ready to be signed
    val newTxOuts: NonEmptyList[TxOut] = NonEmptyList.of(
      TxOut(
        paymentAddress,
        utxo.lovelace - fee,
        nativeAssets.toList
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
      outFile = signedTx,
      testnet = true
    )

    // submit the transaction
    cardano.submitTx(signedTx, testnet = true)
  }

  val cardano = CardanoCli(Paths.get("/Users/roman/Downloads/cardano-node-1.32.1-macos (2)/cardano-cli"))
    .withCardanoNodeSocketPath(Paths.get("/Users/roman/Library/Application Support/Daedalus Testnet/cardano-node.socket").toString)
    .withSudo(false)

  val wd = Files.createDirectories(Paths.get("test12345"))

  def fileFactory: String => File = wd.resolve(_).toFile

  // payment verification and signing keys are the first keys we need to create
  val paymentVerKey: File = fileFactory("payment.vkey")
  val paymentSignKey: File = fileFactory("payment.skey")

  // val paymentAddr: String = cardano.genPaymentKeysAndAddress(paymentVerKey, paymentSignKey, testnet = true)

  // println(s"Please fund this address: $paymentAddr")
  "addr_test1vz4avxtgfzw3tjl4aeaqn9rsce0smln6lgp8tgj6u8daqhg2s7udv"

  import scala.concurrent.duration._
 // Thread.sleep(2.minutes.toMillis)

//  mint(
//    tokenName = "TestTokenPSG-4",
//    tokenAmount = 160,
//    cardano,
//    paymentVerKey,
//    paymentSignKey,
//    paymentAddr,
//    fileFactory
//  )

  def sendToken(
    fromAddress: String,
    tokenName: String,
    tokenAmount: Int,
    toAddress: String,
    paymentSignKey: File,
    txFile: File
  ): Unit = {

    val base16TokenName = Base16String(tokenName)

    val utxo = cardano
      .utxo(fromAddress, testnet = true)
      .find(_.assets.exists(_.tokenName == base16TokenName))
      .getOrElse(throw new RuntimeException("token not found"))

    val asset = utxo.assets
      .find(_.tokenName == base16TokenName)
      .filter(_.tokenAmount > tokenAmount)
      .getOrElse(throw new RuntimeException("insufficient token amount"))

    val toTransfer = asset.copy(tokenAmount = tokenAmount)
    val change = asset.copy(tokenAmount = asset.tokenAmount - tokenAmount)

    val txIns = NonEmptyList.of(
      TxIn(utxo.txHash, utxo.txIx)
    )

    val txOuts = NonEmptyList.of(
      TxOut(
        address = toAddress,
        output = 1500000,
        assets = toTransfer :: Nil
      ),
      TxOut(
        address = fromAddress,
        output = utxo.lovelace,
        assets = change :: Nil
      )
    )

    cardano.buildTx(
      fee = 0,
      txIns = txIns,
      txOuts = txOuts,
      outFile = txFile
    )

    val protocolFile = fileFactory("protocol.json")
    cardano.protocolParams(protocolFile, testnet = true)

    val fee = cardano.calculateFee(
      txBody = txFile,
      protocolParams = protocolFile,
      txInCount = txIns.size,
      txOutCount = txOuts.size,
      witnessCount = 0,
      testnet = true
    )

    val newTxOuts = NonEmptyList.of(
      TxOut(
        address = toAddress,
        output = 1500000,
        assets = toTransfer :: Nil
      ),
      TxOut(
        address = fromAddress,
        output = utxo.lovelace - fee - 1500000,
        assets = change :: Nil
      )
    )

    cardano.buildTx(
      fee = fee,
      txIns = txIns,
      txOuts = newTxOuts,
      outFile = txFile
    )

    val signed = fileFactory("signed-tx-to-transfer")

    cardano.signTx(
      keys = NonEmptyList.of(paymentSignKey),
      txBody = txFile,
      outFile = signed,
      testnet = true,
    )

    cardano.submitTx(signed, testnet = true)
  }

  println(cardano.utxo("addr_test1vz4avxtgfzw3tjl4aeaqn9rsce0smln6lgp8tgj6u8daqhg2s7udv", true))


  def formTxIns(
                utxos: List[UTXO],
                outs: List[TxOut],
                change: (Long, List[NativeAsset]) => Option[TxOut]
              ): (List[TxIn], Option[TxOut]) = {
    import cats.implicits._

    val (totalLovelace, nativeAssets, tokenPolicies) = {
      outs
        .map(o => (
          o.output,
          o.assets.map(a => (a.tokenName, a.tokenAmount)).toMap,
          o.assets.map(a => (a.tokenName, a.policyId))
        ))
        .combineAll
    }

    val tokenToPolicy = tokenPolicies.toMap

    val sortedUTXOs = utxos.sortBy(utxo => (utxo.lovelace, utxo.assets.map(_.tokenAmount))).toVector

    var first = true

    val txOuts = sortedUTXOs
      .scanLeft((0L, Map.empty[Base16String, Long])) {
        (acc, utxo) =>
          acc.combine((utxo.lovelace, utxo.assets.map(a => (a.tokenName, a.tokenAmount)).toMap))
      }
      .tail
      .zip(sortedUTXOs)
      .takeWhile {
        case ((lovelace, tokenToAmount), _) =>
           lovelace < totalLovelace ||
            nativeAssets.exists {
              case (token, amount) =>
                tokenToAmount
                  .get(token)
                  .fold(true)(_ < amount)
            } || {
            val x = first
            first = false
            x
          }

      }

    val txIns = txOuts
      .map { case (_, utxo) => utxo }
      .map(t => TxIn(t.txHash, t.txIx))

    val (consumedLovelace, consumedAssets) = txOuts.last._1

    val lovelaceChange = consumedLovelace - totalLovelace
    println(consumedLovelace)
    println(totalLovelace)

    val assetsChange = consumedAssets.map {
      case (token, amount) =>
        val tokenChange = nativeAssets
          .get(token)
          .fold(amount)(amount - _)

        NativeAsset(token, tokenChange, tokenToPolicy(token))
    }.toList

    (txIns.toList, change(lovelaceChange, assetsChange))
  }


  val d = formTxIns(
    List(
      UTXO("asdasd", 1, 150),
      UTXO("22123123", 2, 2),
    ),
    List(
      TxOut("address_1", 1)
    ),
    (x1, x2) => Some(TxOut("addr_change", x1, x2))
  )

  def burnToken(
    fromAddress: String,
    tokenName: String,
    tokenAmount: Int,
    signKey: File,
    policy: File,
    policyKey: File,
    txFile: File
  ): Unit = {

    val base16TokenName = Base16String(tokenName)

    val utxo = cardano
      .utxo(fromAddress, testnet = true)
      .find(_.assets.exists(_.tokenName == base16TokenName))
      .getOrElse(throw new RuntimeException("token not found"))

    val asset = utxo.assets
      .find(_.tokenName == base16TokenName)
      .filter(_.tokenAmount > tokenAmount)
      .getOrElse(throw new RuntimeException("insufficient token amount"))

    val left = asset.tokenAmount - tokenAmount

    val txIns = NonEmptyList.of(TxIn(utxo.txHash, utxo.txIx))
    val txOuts = NonEmptyList.of(TxOut(fromAddress, utxo.lovelace, asset.copy(tokenAmount = left) :: Nil))

    cardano.buildTx(
      fee = 0,
      txIns = txIns,
      txOuts = txOuts,
      maybeMinting = Some((NonEmptyList.of(asset.copy(tokenAmount = -tokenAmount)), policy)),
      outFile = txFile
    )

    val protocolFile = fileFactory("protocol.json")
    cardano.protocolParams(protocolFile, testnet = true)

    val fee = cardano.calculateFee(txFile, protocolFile, txIns.size, txOuts.size, 2, true)

    val newTxOuts = NonEmptyList.of(TxOut(fromAddress, utxo.lovelace - fee, asset.copy(tokenAmount = left) :: Nil))
    cardano.buildTx(
      fee = fee,
      txIns = txIns,
      txOuts = newTxOuts,
      maybeMinting = Some((NonEmptyList.of(asset.copy(tokenAmount = -tokenAmount)), policy)),
      outFile = txFile
    )

    val signedTx = fileFactory("signed-burning-tx")
    cardano.signTx(
      NonEmptyList.of(signKey, policyKey),
      txFile,
      signedTx,
      true
    )

    cardano.submitTx(signedTx, true)
  }


//  sendToken(
//    fromAddress = "addr_test1vz4avxtgfzw3tjl4aeaqn9rsce0smln6lgp8tgj6u8daqhg2s7udv",
//    tokenName = "TestTokenPSG-4",
//    tokenAmount = 5,
//    toAddress = "addr_test1qzrcqxz4kmlngqceeru58vc5cragym0pv5gdh6v0t0r5yac67xch0swzl6qyheq3zmvysnw775lva5cjcganffnvdn8qp7k6cs",
//    paymentSignKey = paymentSignKey,
//    txFile = fileFactory("transfer-tx-file")
//  )

  burnToken(
    fromAddress = "addr_test1vz4avxtgfzw3tjl4aeaqn9rsce0smln6lgp8tgj6u8daqhg2s7udv",
    tokenName = "TestTokenPSG-4",
    tokenAmount = 10,
    paymentSignKey,
    fileFactory("policy.script"),
    fileFactory("policy.skey"),
    fileFactory("burning-tx")
  )
}

