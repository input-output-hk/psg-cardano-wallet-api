package iog.psg.cardano.util

import com.typesafe.config.Config

import java.io.File
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.language.reflectiveCalls
import scala.sys.process.{Process, ProcessBuilder, ProcessLogger}


case class ProcessBuilderHelper(
                              command: Seq[String] = Seq.empty,
                              parameters: Seq[String] = Seq.empty,
                              env: Map[String, String] = Map.empty,
                              workingDirectory: Option[File] = None,
                              prevCmd: Option[ProcessBuilderHelper] = None) {
  def withCommand(cmd: String): ProcessBuilderHelper = {
    copy(command = command :+ cmd)
  }

  def withPreviousCmd(prev: ProcessBuilderHelper): ProcessBuilderHelper = {
    copy(prevCmd = Some(prev))
  }

  def withWorkingDirectory(wd: File): ProcessBuilderHelper = {
    copy(workingDirectory = Some(wd))
  }

  def withParam(param: String): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param)    }

  def withParam(param: String, value: String): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param :+ value)
  }

  def withParam(param: String, value: File): ProcessBuilderHelper = {
    copy(parameters = parameters :+ param :+ value.getPath)
  }

  def withEnv(envName: String, value: String): ProcessBuilderHelper = {
    copy(env = env + (envName -> value))
  }


  lazy val processBuilder: ProcessBuilder = {
    val p = Process(command ++ parameters, workingDirectory, extraEnv = env.toSeq: _*)
    prevCmd.map(_.processBuilder.#|(p)).getOrElse(p)
  }

}


