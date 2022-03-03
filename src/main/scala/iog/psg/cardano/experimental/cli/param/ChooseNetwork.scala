package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.api.NetworkChooser
import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

trait ChooseNetwork {
  self: CliCmdBuilder =>

  def withNetwork(implicit networkChooser: NetworkChooser): Out =
    build(networkChooser.withNetwork)

}
