package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCli(builder: ProcessBuilderHelper) extends CliCmd {

  lazy val key: CardanoCliCmdKey = {
    CardanoCliCmdKey(builder.withCommand("key"))
  }

  lazy val address: CardanoCliCmdAddress = {
    CardanoCliCmdAddress(builder.withCommand("address"))
  }

  lazy val query: CardanoCliCmdQuery = {
    CardanoCliCmdQuery(builder.withCommand("query"))
  }

  lazy val transaction: CardanoCliCmdTransaction = {
    CardanoCliCmdTransaction(builder.withCommand("transaction"))
  }

  def withCardanoNodeSocketPath(path: String): CardanoCli = {
    copy(builder.withEnv("CARDANO_NODE_SOCKET_PATH", path))
  }

  def withSudo: CardanoCli = {
    copy(builder.withSudo)
  }
}

object CardanoCli {
  private val default: CardanoCli = CardanoCli("./cardano-cli")

  def apply(): CardanoCli = default

  def apply(pathToCardanoCli: String): CardanoCli = {
    CardanoCli(
      ProcessBuilderHelper()
        .withCommand(pathToCardanoCli)
    )
  }
}
