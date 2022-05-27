package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.util.{CliCmd, ProcessBuilderHelper}

case class CardanoCliCmdTransaction(protected val builder: ProcessBuilderHelper) extends CliCmd {

  lazy val calculateMinFee: CardanoCliCmdTransactionMinFee =
    CardanoCliCmdTransactionMinFee(builder.withCommand("calculate-min-fee"))

  lazy val buildRaw: CardanoCliCmdTransactionBuildRaw =
    CardanoCliCmdTransactionBuildRaw(builder.withCommand("build-raw"))

  lazy val witness: CardanoCliCmdTransactionWitness =
    CardanoCliCmdTransactionWitness(builder.withCommand("witness"))

  lazy val assemble: CardanoCliCmdTransactionAssemble =
    CardanoCliCmdTransactionAssemble(builder.withCommand("assemble"))

  lazy val submit: CardanoCliCmdTransactionSubmit =
    CardanoCliCmdTransactionSubmit(builder.withCommand("submit"))

  lazy val policid: CardanoCliCmdTransactionPolicyId =
    CardanoCliCmdTransactionPolicyId(builder.withCommand("policyid"))

  lazy val sign: CardanoCliCmdTransactionSign =
    CardanoCliCmdTransactionSign(builder.withCommand("sign"))

  lazy val txId: CardanoCliCmdTransactionId =
    CardanoCliCmdTransactionId(builder.withCommand("txid"))

  lazy val view: CardanoCliCmdTransactionView =
    CardanoCliCmdTransactionView(builder.withCommand("view"))
}
