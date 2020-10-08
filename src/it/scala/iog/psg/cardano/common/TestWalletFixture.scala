package iog.psg.cardano.common

import iog.psg.cardano.{TestWalletsConfig, WalletConfig}

abstract class TestWalletFixture(walletNum: Int) {
  val wallet: WalletConfig = TestWalletsConfig.walletsMap(walletNum)

  val testWalletName = wallet.name
  val testWalletId = wallet.id
  val testWalletPassphrase = wallet.passphrase
  val testWalletMnemonic = wallet.mnemonic
  val testWalletMnemonicSecondary = wallet.mnemonicSecondary
  val testAmountToTransfer = wallet.amount
  val testMetadata = wallet.metadata
}
