package iog.psg.cardano

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps._
import iog.psg.cardano.CardanoApi._
import iog.psg.cardano.CardanoApiCodec.TxState.TxState
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, GenericMnemonicSentence, Payment, Payments, QuantityUnit, SyncState, TxState, Units}
import iog.psg.cardano.CardanoApiMain.CmdLine
import iog.psg.cardano.util.{ArgumentParser, Configure, Trace}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Failure, Success, Try}

class CardanoApiMainSpec extends AnyFlatSpec with Matchers with Configure {


  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = system.dispatcher
  private implicit val ioEc = IOExecutionContext(context)
  private val baseUrl = config.getString("cardano.wallet.baseUrl")
  private val testWalletName = config.getString("cardano.wallet.name")
  private val testWalletMnemonic = config.getString("cardano.wallet.mnemonic")
  private val testWalletId = config.getString("cardano.wallet.id")

  private val defaultArgs = Array(CmdLine.baseUrl, baseUrl)

  private def makeArgs(args: String*): Array[String] =
    defaultArgs ++ args

  private def runCmdLine(args: String*): Seq[String] = {
    val arguments = new ArgumentParser(makeArgs(args: _*))

    var results: Seq[String] = Seq.empty
    implicit val memTrace = new Trace {
      override def apply(s: Object): Unit = results = s.toString +: results

      override def close(): Unit = ()
    }

    CardanoApiMain.run(arguments)

    results.reverse
  }

  "The Cmd line Main" should "support netInfo" in {
    val results = runCmdLine(CmdLine.netInfo)
    assert(results.exists(_.contains("ready")), "Testnet API service not ready.")
  }

  it should "not create a wallet with a bad mnemonic" in {
    val badMnem = "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21"
    val results = runCmdLine(
      CmdLine.createWallet,
      CmdLine.passphrase, "password",
      CmdLine.name, "some name",
      CmdLine.mnemonic, badMnem)
    assert(results.exists(_.contains("Found an unknown word")), "Bad menmonic not stopped")
  }

   it should "find our test wallet" in {
    val wallets = runCmdLine(
      CmdLine.listWallets)

    wallets.find(_.contains(testWalletName)).getOrElse {
      val results = runCmdLine(
        CmdLine.createWallet,
        CmdLine.passphrase, "password10",
        CmdLine.name, testWalletName,
        CmdLine.mnemonic, testWalletMnemonic)

      assert(results.exists(_.contains(testWalletId)), "Testnet API service not ready.")
    }

  }

  it should "get our wallet" in {
    val results = runCmdLine(
      CmdLine.getWallet,
      CmdLine.passphrase, "password10",
      CmdLine.name, testWalletName)

    assert(results.exists(_.contains(testWalletId)), "Testnet API service not ready.")

  }

}
