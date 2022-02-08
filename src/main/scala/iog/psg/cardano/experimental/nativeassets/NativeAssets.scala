package iog.psg.cardano.experimental.nativeassets

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.command.CardanoCli
import org.apache.commons.codec.binary.Base16

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object NativeAssets extends App {

  case class CardanoCliConfig(
                               sudo: Boolean = false,
                               testnetMagic: Option[Long] = None
                             )

  import iog.psg.cardano.experimental.cli.implicits._

  def mint(
            tokenName: String,
            cardano: CardanoCli,
            workingDir: Path
          ) = {
    val base16 = new Base16()

    val tokenBase16 = base16.encodeToString(tokenName.getBytes(StandardCharsets.UTF_8))

    def path(value: String): Path = workingDir.resolve(value)

    // for our transaction calculations, we need some of the current protocol parameters
    val protocolPath: Path = path("protocol.json")

    cardano
      .query
      .protocolParameters
      .testnetMagic
      .outFile(protocolPath.toFile)
      .exitValue()

    // payment verification and signing keys are the first keys we need to create
    val paymentVerKey: Path = path("payment.vkey")
    val paymentSignKey: Path = path("payment.skey")

    cardano.keyGen(paymentVerKey.toFile, paymentSignKey.toFile)

    // those two keys can now be used to generate an payment address
    val paymentAddr: String =
      cardano
        .address
        .build
        .paymentVerificationKeyFile(paymentVerKey.toFile)
        .testnetMagic
        .res()

    // generate policy verification and signing keys
    val policyVerKey = path("policy.vkey")
    val policySignKey = path("policy.skey")

    cardano.keyGen(policyVerKey.toFile, policySignKey.toFile)

    val paymentVerKeyHash = cardano.keyHash(paymentVerKey.toFile)
    val policyVerKeyHash = cardano.keyHash(policyVerKey.toFile)

    val policy = Policy.All(
      NonEmptyList.of(
        Policy.Script(paymentVerKeyHash),
        Policy.Script(policyVerKeyHash),
      )
    )

    val policyScript = path("policy.script")

    policy.saveTo(policyScript)

    val policyId: String = cardano.policyId(policyScript.toFile)

    val mintTx = path("tx.raw")
    val txIn = ""
    val txOut = ""

    // build raw transaction to calculate fee
    cardano
      .transaction
      .buildRaw
      .fee(0)
      .txIn(txIn)
      .txOut(txOut)
      .mint("") // todo
      .mintScriptFile(policyScript.toFile)
      .outFile(mintTx.toFile)
      .exitValue()

    // fee calculation
    val fee = cardano
      .transaction
      .calculateMinFee
      .txBodyFile(mintTx.toFile)
      .txInCount(1)
      .txOutCount(1)
      .witnessCount(2)
      .testnetMagic
      .protocolParamsFile(protocolPath.toFile)
      .res()
      .toInt

    // re-build the transaction with real fee, ready to be signed
    cardano
      .transaction
      .buildRaw
      .fee(fee)
      .txIn(txIn)
      .txOut(txOut)
      .mint("") // todo
      .mintScriptFile(policyScript.toFile)
      .outFile(mintTx.toFile)
      .exitValue()

    val signedTx = path("tx.signed")

    // sign the transaction
    cardano
      .transaction
      .sign
      .signingKeyFile(paymentSignKey.toFile)
      .signingKeyFile(policySignKey.toFile)
      .testnetMagic
      .txBodyFile(mintTx.toFile)
      .outFile(signedTx.toFile)
      .exitValue()

    // submit the transaction
    cardano
      .transaction
      .submit
      .txFile(signedTx.toFile)
      .testnetMagic
      .exitValue()
  }

  println(Paths.get("/Users/roman/Documents/iohk/cardano-node-docker/ipc/node.socket").toString)

  val cardano = CardanoCli(Paths.get("/Users/roman/Downloads/cardano-node-1.32.1-macos (2)/cardano-cli"))
    .withCardanoNodeSocketPath(Paths.get("/Users/roman/Documents/iohk/cardano-node-docker/ipc/node.socket").toString)
    .withSudo(true)

  val wd = Files.createDirectories(Paths.get("test123"))

  // mint("Testtoken", cardano, wd)

  val addressUtxo = Paths.get("test123/utxoasd")

  println(
    cardano
      .query
      .utxo
      .address("addr_test1vpcp7h5kalavvd096cjltqasdnkc5h054yxwk67dcww8ltcpkzv3e")
      .testnetMagic
      .stringRepr
  )

  import io.circe._
  import io.circe.syntax._

  println(
    Files
      .readString(addressUtxo)
      .asJson
  )
}

