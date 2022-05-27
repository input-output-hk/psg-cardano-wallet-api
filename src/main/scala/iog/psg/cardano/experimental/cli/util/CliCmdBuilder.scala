package iog.psg.cardano.experimental.cli.util

import cats.Foldable

import iog.psg.cardano.experimental.cli.param.ParamValueEncoder

trait CliCmdBuilder extends CliCmd {
  type Out

  def optional[T](paramBuilder: this.type => T => Out)(value: Option[T]): Out =
    value.fold(asOut)(paramBuilder(this)(_))

  protected def withBuilder(b: ProcessBuilderHelper): Out

  protected def build(f: ProcessBuilderHelper => ProcessBuilderHelper): Out =
    withBuilder(f(builder))

  protected def withParam(param: String): Out =
    build(_.withParam(param))

  protected def withParam[V: ParamValueEncoder](param: String, value: V): Out =
    build(_.withParam(param, value))

  protected def withParams[C[_]: Foldable, V: ParamValueEncoder](param: String, values: C[V]): Out =
    build(_.withParams(param, values))

  protected lazy val asOut: Out = build(identity)
}
