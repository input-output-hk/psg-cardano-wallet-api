package iog.psg.cardano.experimental.cli.util


import iog.psg.cardano.experimental.cli.command.{CardanoCli, CardanoCliCmdAddressKeyGen, CardanoCliCmdAddressKeyGenNormalKey}
import iog.psg.cardano.experimental.cli.param.{OutFile, PaymentScriptFile, PaymentVerificationKeyFile, SigningKeyFile}

import java.io.File
import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import scala.util.Using


case class CliSession private (
                       private val workingFolder: Path = Paths.get("."),
                       private val network: NetworkChooser = NetworkChooser.DefaultTestnet
                     ) extends AutoCloseable {

  private def fileParam[T](fileName:String): FileParam[T] = new FileParam[T] {
    val file: File = new File(fileName)

    override def toString: String = {
      val f = workingFolder.resolve(fileName).toFile
      Using.resource(Source.fromFile(f))(_.mkString)
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

  implicit val networkChooser = network

  def withNetwork(whichNetwork: NetworkChooser): CliSession = {
    copy(network = whichNetwork)
  }

  def setPaymentScript(paymentScriptFileContent: String):CliSession = {
    //write to file
    Files.writeString(workingFolder.resolve(paymentScriptFileName), paymentScriptFileContent)
    this
  }

  def close(): Unit = {
    //delete working folder
    println("CLOSE")
  }

  def genKeys(key: String)(implicit cardanoCli: CardanoCli): CardanoCliCmdAddressKeyGenNormalKey = {
    cardanoCli
      .address
      .keyGen
      .verificationKeyFile(ramdomFile(content = key))
      .signingKeyFile
      .normalKey

  }


}
