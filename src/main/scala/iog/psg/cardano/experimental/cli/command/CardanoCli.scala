package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.command
import iog.psg.cardano.experimental.cli.util.{CliCmd, CliCmdBuilder, ProcessBuilderHelper}

import java.nio.file.Path



case class CardanoCli(val builder: ProcessBuilderHelper) extends CliCmd {

  lazy val help: CardanoCliCmdHelp = command.CardanoCliCmdHelp(builder.withCommand("help"))
  lazy val key: CardanoCliCmdKey = command.CardanoCliCmdKey(builder.withCommand("key"))
  lazy val address: CardanoCliCmdAddress = command.CardanoCliCmdAddress(builder.withCommand("address"))
  lazy val query: CardanoCliCmdQuery = command.CardanoCliCmdQuery(builder.withCommand("query"))
  lazy val transaction: CardanoCliCmdTransaction = command.CardanoCliCmdTransaction(builder.withCommand("transaction"))

  val CARDANO_NODE_SOCKET_PATH = "CARDANO_NODE_SOCKET_PATH"

  def withCardanoNodeSocketPath(path: String): CardanoCli =
    CardanoCli(builder.withEnv(CARDANO_NODE_SOCKET_PATH, path))

  def withSudo(value: Boolean = true): CardanoCli =
    CardanoCli(builder.withSudo(value))


}

object CardanoCli {
  private val default: CardanoCli = CardanoCli("./cardano-cli")

  def apply(builder: ProcessBuilderHelper): CardanoCli = new CardanoCli(builder)

  def apply(): CardanoCli = default

  def apply(pathToCardanoCli: String): CardanoCli = {
    CardanoCli(
      ProcessBuilderHelper()
        .withCommand(pathToCardanoCli)
    )
  }

  def apply(pathToCardanoCli: Path): CardanoCli = {
    apply(pathToCardanoCli.toString)
  }
}
