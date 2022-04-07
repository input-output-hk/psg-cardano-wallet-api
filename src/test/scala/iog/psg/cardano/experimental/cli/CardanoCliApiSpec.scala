package iog.psg.cardano.experimental.cli

import cats.data.NonEmptyList
import iog.psg.cardano.experimental.cli.api.Ops.CliApiReqOps
import iog.psg.cardano.experimental.cli.api._
import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.model.{Key, Policy, TxIn, TxOut}
import iog.psg.cardano.experimental.cli.util.RandomFolderFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Files
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.sys.process


class CardanoCliApiSpec extends AnyFlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll{

  val cardanoCli = CardanoCli()
  implicit val networkChooser: NetworkChooser = NetworkChooser.DefaultTestnet
  import concurrent.ExecutionContext.Implicits.global
  val factory = RandomFolderFactory(Files.createTempDirectory("tests"))
  implicit val root = factory.folder

  override def afterAll(): Unit = {
    super.afterAll()
    factory.close()
  }

  var list: List[String] = List.empty

  def listShouldBe(str: String*): Unit = {
    list shouldBe str.toList
    list = List.empty
  }

  def listShouldBeLike(str: String*): Unit = {
    list.zip(str.toList).foreach {
      case (a, b) =>
        val aa = a.split(",").toList
        val bb = b.split(",").toList
        (aa zip bb).foreach {
          case (a, b) if a == b =>
          case (_, b) if b.contains("-SKIP-") =>
          case (a, b) =>
            assert(false, s"$a not equal $b")
        }
    }
    list = List.empty
  }
  implicit val dummyRunner: ProcessBuilderRunner = new ProcessBuilderRunner {

    def addCmdToList(processBuilder: process.ProcessBuilder): Unit = {
      list = list :+ processBuilder.toString
    }

    override def runString(processBuilder: process.ProcessBuilder): String = {
      addCmdToList(processBuilder)
      ""
    }

    override def runUnit(processBuilder: process.ProcessBuilder): Unit = {
      addCmdToList(processBuilder)
    }

    override def runListString(processBuilder: process.ProcessBuilder): List[String] = {
      addCmdToList(processBuilder)
      list
    }
  }

  implicit val timeout: FiniteDuration = 10.seconds

  val sut = CardanoCliApi(cardanoCli)

  "commands" should "match the expected command lines" in {

    val (paymentVerKey: Key[Verification], policyVerKey: Key[Signing]) = sut
      .generateKeyPair()
      .executeBlockingUnsafe

    val s = s"[./cardano-cli, address, key-gen, --verification-key-file, ${paymentVerKey.file.toString}, --signing-key-file, ${policyVerKey.file.toString}, --normal-key]"
    listShouldBe(s)

    val paymentVerKeyHash = sut
      .hashKey(paymentVerKey)
      .executeBlockingUnsafe

    val policyVerKeyHash = sut
        .hashKey(policyVerKey)
      .executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, address, key-hash, --payment-verification-key-file, ${paymentVerKey.file.toString}]",
      s"[./cardano-cli, address, key-hash, --payment-verification-key-file, ${policyVerKey.file.toString}]",
    )

    val policy = Policy.all(
      NonEmptyList.of(
        Policy.Script.Signature(paymentVerKeyHash),
        Policy.Script.Signature(policyVerKeyHash)
      )
    )

    val policyId = sut
      .policyId(policy)
      .executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, transaction, policyid, --script-file, ${policy.file.toString}]",
    )

    val addr = sut.genPaymentAddress(paymentVerKey)
      .executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, address, build, --payment-verification-key-file, ${paymentVerKey.file.toString}, --testnet-magic, 1097911063]",
    )

    val listUtxos = sut.utxo(addr).executeBlockingUnsafe
    listShouldBe(
      s"[./cardano-cli, query, utxo, --address, , --testnet-magic, 1097911063]",
    )

    val params = sut.protocolParams.executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, query, protocol-parameters, --out-file, ${params.file.toString}, --testnet-magic, 1097911063]",
    )

    val fee = 9
    val index1 = 88
    val index2 = 89
    val tx = sut.buildTx(fee,
      NonEmptyList.of(TxIn("A", index1)),
      NonEmptyList.of(TxOut("B", index2))
    ).executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, transaction, build-raw, --fee, $fee, --tx-in, A#${index1}, --tx-out, B+$index2, --out-file, ${tx.file.toString}]",
    )

    val signedTx = sut
      .signTx(NonEmptyList.of(paymentVerKey, policyVerKey), tx)
      .executeBlockingUnsafe

    listShouldBeLike(
      s"[./cardano-cli, transaction, sign, --signing-key-file, ${paymentVerKey.file.toString}, --signing-key-file, ${policyVerKey.file.toString}, --tx-body-file, ${tx.file.toString}, --out-file, -SKIP-, --testnet-magic, 1097911063]",
    )

    val calculatedFee = sut
      .calculateFee(tx, params, 1,1,2)
      .executeBlockingUnsafe

    sut.submitTx(signedTx).executeBlockingUnsafe

    listShouldBe(
      s"[./cardano-cli, transaction, calculate-min-fee, --tx-body-file, ${tx.file.toString}, --tx-in-count, 1, --tx-out-count, 1, --witness-count, 2, --testnet-magic, 1097911063, --protocol-params-file, ${params.file.toString}]",
       s"[./cardano-cli, transaction, submit, --tx-file, ${signedTx.file.toString}, --testnet-magic, 1097911063]",
    )

  }


}
