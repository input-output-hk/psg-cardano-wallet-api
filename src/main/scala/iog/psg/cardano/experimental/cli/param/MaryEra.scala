package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

trait MaryEra {
  self: CliCmd with CopyShim =>

  lazy val maryEra: CONCRETECASECLASS =
    copier.copy(builder.withParam("--mary-era"))
}
