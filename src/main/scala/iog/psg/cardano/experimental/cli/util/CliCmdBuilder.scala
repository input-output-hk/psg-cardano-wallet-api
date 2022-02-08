package iog.psg.cardano.experimental.cli.util

trait CliCmdBuilder extends CliCmd {

  type Out

  protected def withBuilder(b: ProcessBuilderHelper): Out

  protected def build(f: ProcessBuilderHelper => ProcessBuilderHelper): Out =
    withBuilder(f(builder))
}
