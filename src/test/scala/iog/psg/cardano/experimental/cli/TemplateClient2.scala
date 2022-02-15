package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.util.{CliSession, NetworkChooser, ProcessBuilderHelper, RandomFolderFactory}

import java.io.File
import java.nio.file.Paths
import scala.util.{Failure, Success, Using}

object TemplateClient2 {

  def main(args: Array[String]): Unit = {

    val builder = ProcessBuilderHelper()
      .withCommand("/home/alan/develop/cardano-cli-workingfolder/cardano-cli")
      .withSudo(false)


    implicit val cli = CardanoCli(builder)

    Using.resource(RandomFolderFactory(Paths.get("."))) { sessionFolder =>

      val session = CliSession(sessionFolder.folder)
        .withNetwork(NetworkChooser.DefaultTestnet)
        .setPaymentScript("SOME SCRIPT")

      import session._

      genKeys.run

      setPaymentScript("DIFFERENT")

      cli
        .address
        .buildScript
        .paymentScriptFile
        .outFile
        .run[String]

    }
    println("END")

  }
}
