package iog.psg.cardano

import iog.psg.cardano.util.Configure

final case class WalletConfig(
  id: String,
  name: String,
  passphrase: String,
  mnemonic: String,
  mnemonicSecondary: Option[String],
  amount: Option[String],
  metadata: Option[String]
)

object TestWalletsConfig extends Configure {

  lazy val baseUrl = config.getString("cardano.wallet.baseUrl")
  lazy val walletsMap = (1 to 3).map { num =>
    num -> loadWallet(num)
  }.toMap

  private def loadWallet(num: Int) = {
    val name = config.getString(s"cardano.wallet$num.name")
    val mnemonic = config.getString(s"cardano.wallet$num.mnemonic")
    val mnemonicSecondary =
      if (config.hasPath(s"cardano.wallet$num.mnemonicsecondary"))
        Some(config.getString(s"cardano.wallet$num.mnemonicsecondary"))
      else None
    val id = config.getString(s"cardano.wallet$num.id")
    val passphrase = config.getString(s"cardano.wallet$num.passphrase")

    val amount =
      if (config.hasPath(s"cardano.wallet$num.amount"))
        Some(config.getString(s"cardano.wallet$num.amount"))
      else None

    val metadata =
      if (config.hasPath(s"cardano.wallet$num.metadata"))
        Some(config.getString(s"cardano.wallet$num.metadata"))
      else None

    WalletConfig(id, name, passphrase, mnemonic, mnemonicSecondary, amount, metadata)
  }
}
