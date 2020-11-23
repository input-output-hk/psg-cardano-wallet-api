package iog.psg.cardano.common

import iog.psg.cardano.{TestWalletsConfig, WalletConfig}

abstract class TestWalletFixture(walletNum: Int) {
  val walletConfig: WalletConfig = TestWalletsConfig.walletsMap(walletNum)

  val testWalletName: String = walletConfig.name
  val testWalletId: String = walletConfig.id
  def getTestWalletPassphraseOrFail: String = walletConfig.passphrase.get
  def getTestWalletMnemonicOrFail: String = walletConfig.mnemonic.get
  def getTestWalletMnemonicSecondaryOrFail: String = walletConfig.mnemonicSecondary.get
  def getTestAmountToTransferOrFail: String = walletConfig.amount.get
  def getTestMetadataOrFail: String = walletConfig.metadata.get
  def getTestWalletPublicKeyOrFail: String = walletConfig.publicKey.get
}
