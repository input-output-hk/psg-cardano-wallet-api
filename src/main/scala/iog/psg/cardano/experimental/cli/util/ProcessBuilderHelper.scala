package iog.psg.cardano.experimental.cli.util

import iog.psg.cardano.experimental.cli.param.ParamValueEncoder

import scala.sys.process.{Process, ProcessBuilder}

case class ProcessBuilderHelper(
  sudo: Boolean = false,
  command: Vector[String] = Vector.empty,
  parameters: Vector[String] = Vector.empty,
  env: Map[String, String] = Map.empty,
) {

  def withSudo(value: Boolean = true): ProcessBuilderHelper =
    copy(sudo = value)

  def withCommand(cmd: String): ProcessBuilderHelper =
    copy(command = command :+ cmd)

  def withParam(param: String): ProcessBuilderHelper =
    copy(parameters = parameters :+ param)

  def withParam[V: ParamValueEncoder](param: String, value: V): ProcessBuilderHelper =
    copy(parameters = parameters :+ param :+ ParamValueEncoder[V].encodeValue(value))

  def withEnv(envName: String, value: String): ProcessBuilderHelper =
    copy(env = env + (envName -> value))

  def toCommand: String = {
    Option
      .when(sudo)("sudo")
      .concat(env.iterator.map { case (k, v) => s"$k=$v" })
      .concat(command)
      .concat(parameters)
      .filter(_.nonEmpty)
      .mkString(" ")
  }

  lazy val processBuilder: ProcessBuilder =
    Process(command ++ parameters, None, extraEnv = env.toSeq: _*)
}
