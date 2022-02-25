package iog.psg.cardano.experimental.cli.model

import io.circe.syntax._

import java.io.File
import java.nio.file.{Files, Path}

final class PolicyOps(private val policy: Policy) extends AnyVal {

  def saveTo(file: File): Unit = saveTo(file.toPath)
  def saveTo(file: Path): Unit = Files.writeString(file, policy.asJson.noSpaces)
}
