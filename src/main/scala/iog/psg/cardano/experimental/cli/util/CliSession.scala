package iog.psg.cardano.experimental.cli.util


import iog.psg.cardano.experimental.cli.command.{CardanoCli, CardanoCliCmdAddressKeyGen, CardanoCliCmdAddressKeyGenNormalKey, CardanoCliCmdQueryProtocol}
import iog.psg.cardano.experimental.cli.param.{OutFile, PaymentScriptFile, PaymentVerificationKeyFile, SigningKeyFile}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import scala.util.Using


case class CliSession private (
  private val workingFolder: Path = Paths.get("."),
  private val network: NetworkChooser = NetworkChooser.DefaultTestnet,
) extends AutoCloseable {

  private def fileParam[T](fileName: String): FileParam[T] = new FileParam[T] {
    override val file: File = workingFolder.resolve(Paths.get(fileName)).toFile

    override def toString: String = {
      Using.resource(Source.fromFile(file))(_.mkString)
    }
  }

  val verificationKeyFileName = "ver.key"
  implicit val verificationKeyFileImplicit = fileParam[CardanoCliCmdAddressKeyGen](verificationKeyFileName)

  val paymentVerificationKeyFileName = "paymentVer.key"
  implicit val paymentVerificationKeyFileImplicit = fileParam[PaymentVerificationKeyFile](paymentVerificationKeyFileName)

  val signingKeyFileName = "signing.key"
  implicit val signingKeyFileImplicit = fileParam[SigningKeyFile](signingKeyFileName)

  val paymentScriptFileName = "payment.script"
  implicit val paymentScriptFile = fileParam[PaymentScriptFile](paymentScriptFileName)

  val outFileName = "out.file"
  implicit val outFiler = fileParam[OutFile](outFileName)

  implicit val networkChooser: NetworkChooser = network

  def withNetwork(whichNetwork: NetworkChooser): CliSession = {
    copy(network = whichNetwork)
  }

  def setPaymentScript(paymentScriptFileContent: String): CliSession = {
    //write to file
    Files.writeString(workingFolder.resolve(paymentScriptFileName), paymentScriptFileContent)
    this
  }

  val policyVerKey = fileParam("policy.vkey")
  val policySignKey = fileParam("policy.skey")
  val protocolParams = fileParam("protocol.json")
  val policyScript = fileParam("policy.script")
  val txRaw = fileParam("tx.raw")
  val txSigned = fileParam("tx.signed")

  def close(): Unit = {
    //delete working folder
    println("CLOSE")
  }

  import iog.psg.cardano.experimental.cli.implicits._

  def genPolicyKeys(implicit cardanoCli: CardanoCli): CardanoCliCmdAddressKeyGenNormalKey = {
    cardanoCli.genKeys(
      verKey = policyVerKey.file,
      signKey = policySignKey.file
    )
  }

  def getProtocolParams(implicit cardanoCli: CardanoCli): CardanoCliCmdQueryProtocol = {
    cardanoCli.protocolParams(protocolParams.file)
  }
}
