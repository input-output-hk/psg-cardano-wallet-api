package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessResult, SSH}
import org.scalatest.funspec.AnyFunSpec

import java.io.File
import java.nio.file.Files
import scala.util.{Random, Using}
import scala.util.Using.Releasable
import org.scalatest.{BeforeAndAfterAll, Ignore}

import java.time.Instant
import scala.io.Source
import scala.sys.process._

@Ignore
class CardanoCliCmdSpec
  extends AnyFunSpec
    with BeforeAndAfterAll {

  private implicit val releaseTmpFile: Releasable[File] = file => Files.delete(file.toPath)

  private val pathToKey = "TODO"
  private val cardanoHost = "TODO"
  private val cardanoCliPath = "TODO"
  private val cardanoNodeSocketPath = "TODO"

  private var workingDirectory: String = _

  private val cardanoCli: CardanoCli = {
    CardanoCli(cardanoCliPath)
      .withCardanoNodeSocketPath(cardanoNodeSocketPath)
      .withSudo(false)
  }

  private def executeRemotely[T: ProcessResult](cmd: String): T = {
    SSH.executeRemotely(
      identityFile = pathToKey,
      host = cardanoHost,
      cmd
    )
  }

  private def executeRemotely[T: ProcessResult](cmd: CliCmd): T = {
    executeRemotely[T](cmd.stringRepr)
  }

  private def randomFile(): File = {
    val file = new File(workingDirectory + "/file-" + Random.nextInt(50000))
    executeRemotely[Unit](s"touch ${file.getPath}")
    file
  }

  private def randomFileWithContent(content: String): File = {
    val file = new File(workingDirectory + "/file-" + Random.nextInt(50000))
    executeRemotely[Unit](s"echo '$content' > ${file.getPath}")
    file
  }

  private def catFile(file: File): String = {
    executeRemotely[List[String]](s"cat $file").mkString
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    workingDirectory = s"cardano-multi-sig-test-ci/${Instant.now().toEpochMilli}"
    executeRemotely[Unit](s"mkdir -p $workingDirectory")
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    executeRemotely[Unit](s"rm -r $workingDirectory")
  }

  describe("./cardano-cli") {
    describe("query") {
      describe("protocol-parameters") {
        it("should get the node's current protocol parameters") {

          val params = executeRemotely[List[String]] {
            cardanoCli
              .query
              .protocolParameters
              .testnetMagic
          }

          assert(io.circe.parser.parse(params.mkString).isRight)
        }
      }
    }

    describe("address") {

      describe("key-gen") {
        it("should generate a payment key pair") {
          val signingKeyFile = randomFile()
          val verificationKeyFile = randomFile()

          val exitCode = executeRemotely[Int] {
            cardanoCli
              .address
              .keyGen
              .signingKeyFile(signingKeyFile)
              .verificationKeyFile(verificationKeyFile)
              .normalKey
          }

          assert(exitCode == 0)
          assert(io.circe.parser.parse(catFile(signingKeyFile)).isRight)
          assert(io.circe.parser.parse(catFile(verificationKeyFile)).isRight)
        }
      }

      describe("key-hash") {
        it("should print the hash of an address key") {
          Using.resource(Source.fromResource("cli/verification-key-file.json")) {
            verificationKeyFile =>
              val hash = executeRemotely[String] {
                cardanoCli
                  .address
                  .keyHash
                  .paymentVerificationKey(verificationKeyFile.getLines.mkString)
              }

              assert(hash.nonEmpty)
          }
        }
      }

      describe("build-script") {
        it("should build a Shelley script address") {
          Using.resource(Source.fromResource("cli/all-multi-sig-script.json")) {
            paymentScriptString =>
              val paymentScriptFile = randomFileWithContent(paymentScriptString.getLines().mkString)

              val address: String = executeRemotely[String] {
                cardanoCli
                  .address
                  .buildScript
                  .paymentScriptFile(paymentScriptFile)
                  .testnetMagic
              }

              assert(address.startsWith("addr_test1"))
          }
        }
      }
    }
  }
}


