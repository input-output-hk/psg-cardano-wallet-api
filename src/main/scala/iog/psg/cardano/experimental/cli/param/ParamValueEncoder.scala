package iog.psg.cardano.experimental.cli.param

import java.io.File
import java.nio.file.Path

trait ParamValueEncoder[T] { self =>

  def apply(t: T): String

  def comap[A](f: A => T): ParamValueEncoder[A] = a => self(f(a))
}

object ParamValueEncoder {
  @inline def apply[T: ParamValueEncoder]: ParamValueEncoder[T] = implicitly

  implicit val string: ParamValueEncoder[String] = identity
  implicit val int: ParamValueEncoder[Int] = _.toString
  implicit val long: ParamValueEncoder[Long] = _.toString
  implicit val file: ParamValueEncoder[File] = _.getPath
  implicit val path: ParamValueEncoder[Path] = file.comap(_.toFile)
}
