package iog.psg.cardano.experimental.cli

import iog.psg.cardano.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdAddress(protected val builder: ProcessBuilderHelper) extends CliCmd {

  lazy val keyHash: CardanoCliCmdAddressKeyHash =
    CardanoCliCmdAddressKeyHash(builder.withParam("key-hash"))

  lazy val keyGen: CardanoCliCmdAddressKeyGen =
    CardanoCliCmdAddressKeyGen(builder.withParam("key-gen"))

  lazy val buildScript: CardanoCliCmdAddressBuildScript =
    CardanoCliCmdAddressBuildScript(builder.withParam("build"))

  lazy val build: CardanoCliCmdAddressBuild =
    CardanoCliCmdAddressBuild(builder.withParam("build"))
}