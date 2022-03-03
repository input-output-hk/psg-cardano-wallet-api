package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmd

trait CanRun {

  self: CliCmd =>

  def processBuilder = builder.processBuilder
}
