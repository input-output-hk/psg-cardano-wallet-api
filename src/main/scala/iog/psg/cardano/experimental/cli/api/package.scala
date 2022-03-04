package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.util.{RandomFolderFactory, RandomTempFolder}

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.sys.process.ProcessBuilder
import scala.util.{Random, Try}


package object api {

  sealed trait KeyType
  trait Verification extends KeyType
  trait Signing extends KeyType

  trait IsKey[A <: KeyType]
  trait IsKeyHash[A <: KeyType]

  trait CliApiRequest[B] {
    def execute: Future[B]
  }

  trait ProcessBuilderRunner {
    def runString(processBuilder: ProcessBuilder): String
    def runUnit(processBuilder: ProcessBuilder): Unit
    def runListString(processBuilder: ProcessBuilder): List[String]
  }

  private def generateRandomFileName(): String = Random.nextLong(Long.MaxValue).toString

  trait IsFile {
    implicit val rootFolder: RandomTempFolder
    lazy val fileName: String = generateRandomFileName()
    lazy val file: File = rootFolder.value.resolve(fileName).toFile

  }

  trait InFile extends IsFile {
    val content: String

    override lazy val file: File = {
      val f = rootFolder.value.resolve(fileName).toFile
      Files.writeString(f.toPath, content)
      f
    }
  }

  trait OutFile extends IsFile

  object Ops {

    implicit class CliApiReqOps[A](val a: CliApiRequest[A]) extends AnyVal {
      def executeBlocking(implicit timeout: FiniteDuration): Try[A] = {
        Try(Await.result(a.execute, timeout))
      }

      def executeBlockingUnsafe(implicit timeout: FiniteDuration): A = {
        executeBlocking.get
      }
    }

    implicit class ReadFrom(outFile: OutFile) {

      def read: String = {
        outFile.file.read
      }
    }

    implicit class ReadFromFile(file: File) {

      def read: String = {
        val bufferedSource = Source.fromFile(file)
        try {
          bufferedSource.getLines().mkString
        } finally {
          bufferedSource.close
        }
      }
    }

  }
}
