package iog.psg.cardano.experimental.cli.util

import java.nio.file.{Files, Path}
import scala.util.Random

case class RandomFolderFactory(private val rootFolder: Path) extends AutoCloseable{

  Files.createDirectories(rootFolder)

  require(rootFolder.toFile.exists(), s"Failed to create $rootFolder")
  require(rootFolder.toFile.isDirectory, s"$rootFolder is not a folder")

  val folder: Path =
    Files.createDirectory(rootFolder.resolve(Random.nextLong(Long.MaxValue).toString))

  override def close(): Unit = {
    folder.toFile.listFiles().foreach(_.delete())
    Files.delete(folder)
  }
}
