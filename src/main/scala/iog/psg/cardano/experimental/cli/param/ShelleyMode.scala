package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

trait ShelleyMode {
  self: CliCmdBuilder =>

  lazy val shelleyMode: Out =
    build(_.withParam("--shelley-mode"))
}
