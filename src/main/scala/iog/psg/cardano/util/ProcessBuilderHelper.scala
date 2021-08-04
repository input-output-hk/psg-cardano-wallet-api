package iog.psg.cardano.util

import java.io.File
import scala.sys.process.Process
import scala.sys.process.ProcessBuilder

case class ProcessBuilderHelper(
  sudo: Boolean = false,
  command: Vector[String] = Vector.empty,
  parameters: Vector[String] = Vector.empty,
  env: Map[String, String] = Map.empty,
) {

  def withSudo: ProcessBuilderHelper = {
    copy(sudo = true)
  }

  def withCommand(cmd: String): ProcessBuilderHelper = {
    copy(command = command :+ cmd)
  }

  def withParam(param: String): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param)
  }

  def withParam(param: String, value: String): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param :+ value)
  }

  def withParam(param: String, value: File): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param :+ value.getPath)
  }

  def withEnv(envName: String, value: String): ProcessBuilderHelper = {
    copy(env = env + (envName -> value))
  }

  def toCommand: String = {
    (if (sudo) "sudo " else "") +
      env.iterator.map { case (k, v) => s"$k=$v" }.mkString(start = "", sep = " ", end = " ") +
      (command ++ parameters).mkString(" ")
  }

  lazy val processBuilder: ProcessBuilder = {
    Process(command ++ parameters, None, extraEnv = env.toSeq: _*)
  }
}

