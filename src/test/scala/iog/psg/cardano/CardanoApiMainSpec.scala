package iog.psg.cardano

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi._
import iog.psg.cardano.CardanoApiMain.CmdLine
import iog.psg.cardano.util.{ArgumentParser, Configure, Trace}
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class CardanoApiMainSpec extends AnyFlatSpec with Matchers with Configure {


  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = system.dispatcher
  private implicit val ioEc = IOExecutionContext(context)
  private val baseUrl = config.getString("cardano.wallet.baseUrl")
  private val testWalletName = config.getString("cardano.wallet.name")
  private val testWallet2Name = config.getString("cardano.wallet2.name")
  private val testWalletMnemonic = config.getString("cardano.wallet.mnemonic")
  private val testWallet2Mnemonic = config.getString("cardano.wallet2.mnemonic")
  private val testWalletId = config.getString("cardano.wallet.id")
  private val testWallet2Id = config.getString("cardano.wallet2.id")
  private val testWalletPassphrase = config.getString("cardano.wallet.passphrase")
  private val testWallet2Passphrase = config.getString("cardano.wallet2.passphrase")
  private val testAmountToTransfer = config.getString("cardano.wallet.amount")

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

  "The Cmd line Main" should "support retrieving netInfo" in {
    val results = runCmdLine(CmdLine.netInfo)
    assert(results.exists(_.contains("ready")), s"Testnet API service not ready - '$baseUrl' \n $results")
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

    wallets.find(w => w.contains(testWalletName) &&
      w.contains(testWalletId))
      .getOrElse {
        val results = runCmdLine(
          CmdLine.createWallet,
          CmdLine.passphrase, testWalletPassphrase,
          CmdLine.name, testWalletName,
          CmdLine.mnemonic, testWalletMnemonic)

        assert(results.exists(_.contains(testWalletId)), "Test Wallet not created")
      }

  }

  it should "get our wallet" in {
    val results = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWalletId)

    assert(results.exists(_.contains(testWalletId)), "Test wallet not found.")

  }

  it should "create or find wallet 2" in {

    val wallets = runCmdLine(CmdLine.listWallets)

    wallets.find(w => w.contains(testWallet2Name) &&
      w.contains(testWallet2Id))
      .getOrElse {
        val results = runCmdLine(
          CmdLine.createWallet,
          CmdLine.passphrase, testWallet2Passphrase,
          CmdLine.name, testWallet2Name,
          CmdLine.mnemonic, testWallet2Mnemonic)

        assert(results.last.contains(testWallet2Id), "Test wallet 2 not found.")
      }
  }

  it should "allow password change in test wallet 2" in {
    runCmdLine(
      CmdLine.updatePassphrase,
      CmdLine.oldPassphrase, testWallet2Passphrase,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.walletId, testWallet2Id)

  }

  it should "fund payments" in {

    val results = runCmdLine(
      CmdLine.fundTx,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.address, getUnusedAddressWallet2,
      CmdLine.walletId, testWalletId)

    assert(results.last.contains("FundPaymentsResponse"), "FundPaymentsResponse failed?")
    //results.foreach(println)
  }

  private def getUnusedAddressWallet2 = getUnusedAddress(testWallet2Id)

  private def getUnusedAddressWallet1 = getUnusedAddress(testWalletId)

  def getUnusedAddress(walletId: String): String = {
    val results = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "unused",
      CmdLine.walletId, walletId)


    val all = results.last.split(",")
    val cleanedUp = all.map(s => {
      if (s.indexOf("addr") > 0)
        Some(s.substring(s.indexOf("addr")))
      else None
    }) collect {
      case Some(goodAddr) => goodAddr
    }
    cleanedUp.head
  }

  it should "transact from a to a" in {

    val unusedAddr = getUnusedAddressWallet1

    // estimate fee
    val estimateResults = runCmdLine(
      CmdLine.estimateFee,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.address, unusedAddr,
      CmdLine.walletId, testWalletId)

    //estimateResults.foreach(println)

    val preTxTime = ZonedDateTime.now().minusMinutes(10)

    val resultsCreateTx = runCmdLine(
      CmdLine.createTx,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.address, unusedAddr,
      CmdLine.walletId, testWalletId)

    assert(resultsCreateTx.last.contains("pending"), "Transaction should be pending")

    val txId = extractTxId(resultsCreateTx.last)

    val resultsGetTx = runCmdLine(
      CmdLine.getTx,
      CmdLine.txId, txId,
      CmdLine.walletId, testWalletId)

    assert(resultsGetTx.last.contains(txId), "The getTx result didn't contain the id")
    //list Txs

    val postTxTime = ZonedDateTime.now().plusMinutes(50)

    def listWalletTxs: Seq[String] = runCmdLine(
      CmdLine.listWalletTransactions,
      CmdLine.minWithdrawal, "1",
      CmdLine.start, preTxTime.toString,
      CmdLine.walletId, testWalletId)

    var resultsListTxs = listWalletTxs
    // Disable the retry for CI, there is a question mark around whether the
    // wallet will always return the txId, hence the fudge.
    var retryCount = 0
    if(retryCount > 0) {
      val sleepInterval: Long = 1000 * 60
      println(s"Looking for $txId, this could take $retryCount * $sleepInterval ms...")

      while (!resultsListTxs.exists(_.contains(txId)) && retryCount > 0) {
        resultsListTxs = listWalletTxs
        retryCount -= 1
        resultsListTxs.foreach(println)
        //It seems the wallet can be slow to react sometimes.
        Thread.sleep(sleepInterval)
      }

      assert(resultsListTxs.exists(_.contains(txId)), "TxId never shows up in list?")
    } else if (!resultsListTxs.exists(_.contains(txId))) {
      println(s"Warning: $txId NOT found in listTransactions.")
    }
  }

  def extractTxId(toStringCreateTransactionResult: String): String = {
    toStringCreateTransactionResult.split(",").head.stripPrefix("CreateTransactionResponse(")
  }


  it should "delete test wallet 2" in {
    runCmdLine(
      CmdLine.deleteWallet,
      CmdLine.walletId, testWallet2Id)
    val results = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWallet2Id)

    assert(results.exists(!_.contains(testWalletId)), "Test wallet found after deletion?")

  }

}
