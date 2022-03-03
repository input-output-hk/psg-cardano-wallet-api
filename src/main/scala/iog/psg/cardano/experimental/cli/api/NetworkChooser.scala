package iog.psg.cardano.experimental.cli.api

import iog.psg.cardano.experimental.cli.util.ProcessBuilderHelper

trait NetworkChooser {
  def withNetwork(processBuilderHelper: ProcessBuilderHelper): ProcessBuilderHelper
}

object NetworkChooser {
  val Mainnet = new NetworkChooser {
    override def withNetwork(processBuilderHelper: ProcessBuilderHelper): ProcessBuilderHelper = {
      processBuilderHelper.withParam("--mainnet")
    }
  }

  val DefaultTestnet = new NetworkChooser {
    override def withNetwork(processBuilderHelper: ProcessBuilderHelper): ProcessBuilderHelper = {
      addTestnetMagic(processBuilderHelper, DefaultTestnetMagicNumber)
    }
  }

  val DefaultTestnetMagicNumber = 1097911063

  private def addTestnetMagic(helper: ProcessBuilderHelper, magicNumber: Long): ProcessBuilderHelper = {
    helper.withParam("--testnet-magic", magicNumber)
  }

  def withTestnetMagic(magic: Long): NetworkChooser = new NetworkChooser {
    override def withNetwork(processBuilderHelper: ProcessBuilderHelper): ProcessBuilderHelper = {
      addTestnetMagic(processBuilderHelper, magic)
    }
  }
}
