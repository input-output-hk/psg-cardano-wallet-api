package iog.psg.cardano.experimental.cli.util

import scala.util.matching.Regex

object Regexes {

  val spaces: Regex = "\\s+".r
  val utxoPartSeparator: Regex = "\\s+[+]\\s+".r
}
