package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdQuery(protected val builder: ProcessBuilderHelper) extends CliCmd {

  /*
  protocol-parameters | tip | stake-distribution |
                         stake-address-info | utxo | ledger-state |
                         protocol-state | stake-snapshot | pool-params
   */
  lazy val protocolParameters: CardanoCliCmdQueryProtocol =
    CardanoCliCmdQueryProtocol(builder.withCommand("protocol-parameters"))

  lazy val utxo: CardanoCliCmdQueryUtxo =
    CardanoCliCmdQueryUtxo(builder.withCommand("utxo"))
}
