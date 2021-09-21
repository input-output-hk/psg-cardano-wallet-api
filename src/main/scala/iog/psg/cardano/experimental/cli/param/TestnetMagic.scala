package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.CopyShim
import iog.psg.cardano.util.CliCmd

trait TestnetMagic {
  self: CliCmd with CopyShim =>

  lazy val mainnet: CONCRETECASECLASS = {
    copier.copy(builder.withParam("--mainnet"))
  }

  def testnetMagic(magic: Long): CONCRETECASECLASS = {
    copier.copy(builder.withParam("--testnet-magic", magic.toString))
  }

  def testnetMagic: CONCRETECASECLASS = testnetMagic(1097911063)
}
