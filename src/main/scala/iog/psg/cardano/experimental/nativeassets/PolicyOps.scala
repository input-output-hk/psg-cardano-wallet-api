package iog.psg.cardano.experimental.nativeassets

import java.io.File
import java.nio.file.{Files, Path}
import io.circe.syntax._

final class PolicyOps(private val policy: Policy) extends AnyVal {

  def saveTo(file: File): Unit = saveTo(file.toPath)
  def saveTo(file: Path): Unit = Files.writeString(file, policy.asJson.noSpaces)
}

trait PolicySyntax {

  implicit def toPolicyOps(policy: Policy): PolicyOps =
    new PolicyOps(policy)
}
