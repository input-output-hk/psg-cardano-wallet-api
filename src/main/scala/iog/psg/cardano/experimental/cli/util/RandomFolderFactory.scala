package iog.psg.cardano.experimental.cli.util

import java.nio.file.{Files, Path}
import scala.util.Random

case class RandomTempFolder(value: Path)

case class RandomFolderFactory(private val rootFolder: Path) extends AutoCloseable {

  Files.createDirectories(rootFolder)

  require(rootFolder.toFile.exists(), s"Failed to create $rootFolder")
  require(rootFolder.toFile.isDirectory, s"$rootFolder is not a folder")

  val folder: RandomTempFolder = RandomTempFolder(
    Files.createDirectory(rootFolder.resolve(Random.nextLong(Long.MaxValue).toString))
  )

  override def close(): Unit = {
    folder.value.toFile.listFiles().foreach(_.delete())
    Files.delete(folder.value)
  }
}
