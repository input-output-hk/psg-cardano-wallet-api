package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.api.OutFile
import iog.psg.cardano.experimental.cli.util.RandomTempFolder


case class ProtocolParams private[cli]()(implicit val rootFolder: RandomTempFolder) extends OutFile
