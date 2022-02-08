package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

trait MaryEra {
  self: CliCmdBuilder =>

  lazy val maryEra: Out =
    build(_.withParam("--mary-era"))
}
