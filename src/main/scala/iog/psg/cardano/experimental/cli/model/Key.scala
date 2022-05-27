package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.api.{IsKey, KeyType, OutFile}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder

import java.nio.file.Files


object Key {
  def apply[A <: KeyType](keyAsString: String)(implicit rootFolder: RandomTempFolder): Key[A] = {
    val k = Key[A]()
    Files.writeString(k.file.toPath, keyAsString)
    k
  }
}

case class Key[A <: KeyType] private[cli]()(implicit val rootFolder: RandomTempFolder) extends OutFile with IsKey[A]
