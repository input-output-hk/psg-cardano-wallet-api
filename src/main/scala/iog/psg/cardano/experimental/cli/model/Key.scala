package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.api.{IsKey, KeyType, OutFile}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder


case class Key[A <: KeyType] private[cli]()(implicit val rootFolder: RandomTempFolder) extends OutFile with IsKey[A]
