package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

trait TestnetMagic {
  self: CliCmdBuilder =>

  lazy val mainnet: Out =
    build(_.withParam("--mainnet"))

  def testnetMagic(magic: Long): Out =
    build(_.withParam("--testnet-magic", magic))

  lazy val testnetMagic: Out = testnetMagic(1097911063)
}
