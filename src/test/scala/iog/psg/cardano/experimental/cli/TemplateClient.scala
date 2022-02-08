package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.util.ProcessBuilderHelper

import java.io.File

object TemplateClient {

  def main(args: Array[String]): Unit = {

    val TESTNET_MAGIC = 1097911063
    val workingDirPath = "/home/alan/apps/cardano-cli/"

    def makeFileName(name: String): File = {
      new File(new File(workingDirPath), name)
    }

    val builderSudo = ProcessBuilderHelper()
      .withCommand("echo")
      .withParam(args.head)

    val builder = ProcessBuilderHelper()
      .withCommand("sudo")
      .withCommand("-S")
      .withCommand("CARDANO_NODE_SOCKET_PATH=/var/lib/docker/volumes/cardano-cli_node-ipc/_data/node.socket")
      .withCommand("./cardano-cli")


    val outFile: File = makeFileName("protocol4.json")

    val cli = CardanoCli(builder)
      .query
      .protocolParameters
      .testnetMagic(TESTNET_MAGIC)
      .outFile(outFile)

    val all = cli.res()

    println(all)

    if (outFile.exists()) {
      println("ok")
    }

    CardanoCli(builder)
      .address
      .keyGen
      .verificationKeyFile(makeFileName("payVerKey1"))
      .signingKeyFile(makeFileName("paySignKey1"))
      .normalKey
      .exitValue()

    CardanoCli(builder)
      .address
      .keyGen
      .verificationKeyFile(makeFileName("payVerKey2"))
      .signingKeyFile(makeFileName("paySignKey2"))
      .normalKey
      .exitValue()

    // ./cardano-cli address key-hash --payment-verification-key-file $DIR/payVerKey1 > $DIR/keyHash1
    val hash1 = CardanoCli(builder)
      .address
      .keyHash
      .paymentVerificationKeyFile(makeFileName("payVerKey1"))
      .res()

    val hash2 = CardanoCli(builder)
      .address
      .keyHash
      .paymentVerificationKeyFile(makeFileName("payVerKey2"))
      .res()

    println(s"hash1 $hash1, hash2 $hash2")

    def makeScript(keyHash1: String, keyHash2: String): String = {
      s"""{ "type": "all", "scripts": [ { "type": "sig", "keyHash": "${keyHash1}" }, { "type": "sig", "keyHash": "${keyHash2}" } ] }"""
    }

    import java.nio.file.Files
    import java.nio.file.Paths

    Files.write(makeFileName("allMultiSigScript").toPath, makeScript(hash1, hash2).getBytes)

    CardanoCli(builder)
      .address
      .build
      .paymentScriptFile(makeFileName("allMultiSigScript"))
      .testnetMagic(TESTNET_MAGIC)
      .outFile(makeFileName("script.addr"))
      .exitValue()
  }
}
