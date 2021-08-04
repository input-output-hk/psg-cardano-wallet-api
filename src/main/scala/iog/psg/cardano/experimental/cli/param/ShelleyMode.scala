package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

trait ShelleyMode {
  self: CliCmd with CopyShim =>

  lazy val shelleyMode: CONCRETECASECLASS =
    copier.copy(builder.withParam("--shelley-mode"))
}
