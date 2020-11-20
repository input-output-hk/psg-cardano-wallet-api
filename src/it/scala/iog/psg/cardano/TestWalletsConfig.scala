package iog.psg.cardano

import iog.psg.cardano.util.Configure

import scala.util.Try

final case class WalletConfig(
  id: String,
  name: String,
  passphrase: Option[String],
  mnemonic: Option[String],
  mnemonicSecondary: Option[String],
  amount: Option[String],
  metadata: Option[String],
  publicKey: Option[String]
)

object TestWalletsConfig extends Configure {

  lazy val baseUrl = config.getString("cardano.wallet.baseUrl")
  lazy val walletsMap = (1 to 4).map { num =>
    num -> loadWallet(num)
  }.toMap

  private def loadWallet(num: Int) = {
    val id = config.getString(s"cardano.wallet$num.id")
    val name = config.getString(s"cardano.wallet$num.name")

    val mnemonic = Try(config.getString(s"cardano.wallet$num.mnemonic")).toOption
    val mnemonicSecondary = Try(config.getString(s"cardano.wallet$num.mnemonicsecondary")).toOption
    val passphrase = Try(config.getString(s"cardano.wallet$num.passphrase")).toOption
    val amount = Try(config.getString(s"cardano.wallet$num.amount")).toOption
    val metadata = Try(config.getString(s"cardano.wallet$num.metadata")).toOption
    val publicKey = Try(config.getString(s"cardano.wallet$num.publickey")).toOption

    WalletConfig(id, name, passphrase, mnemonic, mnemonicSecondary, amount, metadata, publicKey)
  }
}
