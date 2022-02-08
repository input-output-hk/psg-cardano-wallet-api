package iog.psg.cardano.experimental.cli.command

import iog.psg.cardano.experimental.cli.param.ScriptFile
import iog.psg.cardano.experimental.cli.util.{CliCmdBuilder, ProcessBuilderHelper}

case class CardanoCliCmdTransactionPolicyId(protected val builder: ProcessBuilderHelper)
  extends CliCmdBuilder
    with ScriptFile {

  def res(): String = run[String]

  override type Out = CardanoCliCmdTransactionPolicyId
  override protected def withBuilder(b: ProcessBuilderHelper): CardanoCliCmdTransactionPolicyId = copy(b)
}
