package iog.psg.cardano

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApiMain.CmdLine
import iog.psg.cardano.util.{ArgumentParser, Configure, Trace}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiMainITSpec extends AnyFlatSpec with Matchers with Configure with ScalaFutures with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    runCmdLine(
      CmdLine.deleteWallet,
      CmdLine.walletId, testWallet2Id)
    super.afterAll()
  }

  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = system.dispatcher
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
  private val testMetadata = config.getString("cardano.wallet.metadata")

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

    implicit val apiRequestExecutor: ApiRequestExecutor = ApiRequestExecutor

    CardanoApiMain.run(arguments)

    results.reverse
  }

  "The Cmd line -netInfo" should "support retrieving netInfo" in {
    val cmdLineResults = runCmdLine(CmdLine.netInfo)
    assert(cmdLineResults.exists(_.contains("ready")), s"Testnet API service not ready - '$baseUrl' \n $cmdLineResults")
  }

  "The Cmd Line -wallets" should "show our test wallet in the list" in {
    val cmdLineResults = runCmdLine(
      CmdLine.listWallets)

    cmdLineResults.find(w => w.contains(testWalletName) &&
      w.contains(testWalletId))
      .getOrElse {
        val cmdLineResults = runCmdLine(
          CmdLine.createWallet,
          CmdLine.passphrase, testWalletPassphrase,
          CmdLine.name, testWalletName,
          CmdLine.mnemonic, testWalletMnemonic)

        assert(cmdLineResults.exists(_.contains(testWalletId)), "Test Wallet not created")
      }

  }

  "The Cmd Line -estimateFee" should "estimate transaction costs" in {
    val unusedAddr = getUnusedAddressWallet1

    val cmdLineResults = runCmdLine(
      CmdLine.estimateFee,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.address, unusedAddr,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains("EstimateFeeResponse(QuantityUnit(")))
  }

  "The Cmd Line -wallet [walletId]" should "get our wallet" in {
    val cmdLineResults = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains(testWalletId)), "Test wallet not found.")
  }

  "The Cmd Line -createWallet" should "create wallet 2" in {
    val results = runCmdLine(
      CmdLine.createWallet,
      CmdLine.passphrase, testWallet2Passphrase,
      CmdLine.name, testWallet2Name,
      CmdLine.mnemonic, testWallet2Mnemonic)

    assert(results.last.contains(testWallet2Id), "Test wallet 2 not found.")
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

  "The Cmd Line -updatePassphrase" should "allow password change in test wallet 2" in {
    val cmdLineResults = runCmdLine(
      CmdLine.updatePassphrase,
      CmdLine.oldPassphrase, testWallet2Passphrase,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.walletId, testWallet2Id)

    assert(cmdLineResults.exists(_.contains("Unit result from update passphrase")))
  }

  "The Cmd Line -deleteWallet [walletId]" should "delete test wallet 2" in {
    val cmdLineResults = runCmdLine(
      CmdLine.deleteWallet,
      CmdLine.walletId, testWallet2Id)

    assert(cmdLineResults.exists(_.contains("Unit result from delete wallet")))

    val results = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWallet2Id)

    assert(results.exists(!_.contains(testWalletId)), "Test wallet found after deletion?")
  }

  "The Cmd Line -restoreWallet" should "restore deleted wallet 2" in {
    val cmdLineResults = runCmdLine(
      CmdLine.restoreWallet,
      CmdLine.passphrase, testWallet2Passphrase,
      CmdLine.name, testWallet2Name,
      CmdLine.mnemonic, testWallet2Mnemonic)

    assert(cmdLineResults.exists(_.contains(s"Wallet($testWallet2Id")))
  }

  "The Cmd Line -listAddresses -walletId [walletId] -state [state]" should "list unused wallet addresses" in {
    val cmdLineResults = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "unused",
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains("Some(unused)")))
    assert(cmdLineResults.exists(!_.contains("Some(used)")))
  }

  it should "list used wallet addresses" in {
    val cmdLineResults = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "used",
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains("Some(used)")))
    cmdLineResults.count(_.contains("Some(unused)")) shouldBe 0
  }

  "The Cmd Line -fundTx" should "fund payments" in {
    val cmdLineResults = runCmdLine(
      CmdLine.fundTx,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.address, getUnusedAddressWallet2,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.last.contains("FundPaymentsResponse") ||
      cmdLineResults.mkString("").contains("cannot_cover_fee"), s"$cmdLineResults")
  }

  "The Cmd Lines -createTx, -getTx, -listTxs" should "transact from A to B with metadata, txId should be visible in get and list" in {
    val unusedAddr = getUnusedAddressWallet1
    val preTxTime = ZonedDateTime.now().minusMinutes(1)

    val resultsCreateTx = runCmdLine(
      CmdLine.createTx,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.amount, testAmountToTransfer,
      CmdLine.metadata, testMetadata,
      CmdLine.address, unusedAddr,
      CmdLine.walletId, testWalletId)

    assert(resultsCreateTx.last.contains("pending"), "Transaction should be pending")

    val txId = extractTxId(resultsCreateTx.last)

    val resultsGetTx = runCmdLine(
      CmdLine.getTx,
      CmdLine.txId, txId,
      CmdLine.walletId, testWalletId)

    assert(resultsGetTx.last.contains(txId), "The getTx result didn't contain the id")

    val postTxTime = ZonedDateTime.now().plusMinutes(5)

    val resultsListWalletTxs = runCmdLine(
      CmdLine.listWalletTransactions,
      CmdLine.start, preTxTime.toString,
      CmdLine.`end`, postTxTime.toString,
      CmdLine.walletId, testWalletId)

    val foundTx = resultsListWalletTxs.exists(_.contains(txId))
    assert(foundTx, s"Couldn't find txId $txId in transactions ")
  }

  "The Cmd Line --help" should "show possible commands" in {
    val results = runCmdLine(CmdLine.help)

    results.mkString("\n") shouldBe
      """This super simple tool allows developers to access a cardano wallet backend from the command line
        |
        |Usage:
        |export CMDLINE='java -jar psg-cardano-wallet-api-assembly-<VER>.jar'
        |$CMDLINE [command] [arguments]
        |
        |Optional commands:
        |-trace [filename] [command]
        | write logs into a defined file ( default file name: cardano-api.log )
        |
        | Examples:
        | $CMDLINE -trace wallets.log -wallets
        |
        |-baseUrl [url] [command]
        | define different api url ( default : http://127.0.0.1:8090/v2/ )
        |
        | Examples:
        | $CMDLINE -baseUrl http://cardano-wallet-testnet.mydomain:8090/v2/ -wallets
        |
        |-noConsole [command]
        | run a command without any logging
        |
        | Examples:
        | $CMDLINE -noConsole -deleteWallet -walletId 1234567890123456789012345678901234567890
        |
        |Commands:
        |-netInfo
        | Show network information
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkInformation ]
        |
        | Examples:
        | $CMDLINE -netInfo
        |
        |-wallets
        | Return a list of known wallets, ordered from oldest to newest
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listWallets ]
        |
        | Examples:
        | $CMDLINE -wallets
        |
        |-estimateFee -walletId [walletId] -amount [amount] -address [address]
        | Estimate fee for the transaction
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee ]
        |
        | Examples:
        | $CMDLINE -estimateFee -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890
        |
        |-wallet -walletId [walletId]
        | Get wallet by id
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet ]
        |
        | Examples:
        | $CMDLINE -wallet -walletId 1234567890123456789012345678901234567890
        |
        |-updatePassphrase -walletId [walletId] -oldPassphrase [oldPassphrase] -passphrase [newPassphrase]
        | Update passphrase
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWalletPassphrase ]
        |
        | Examples:
        | $CMDLINE -updatePassphrase -walletId 1234567890123456789012345678901234567890 -oldPassphrase OldPassword12345! -passphrase NewPassword12345!]
        |
        |-deleteWallet -walletId [walletId]
        | Delete wallet by id
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteWallet ]
        |
        | Examples:
        | $CMDLINE -deleteWallet -walletId 1234567890123456789012345678901234567890
        |
        |-listAddresses -walletId [walletId] -state [state]
        | Return a list of known addresses, ordered from newest to oldest, state: used, unused
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listAddresses ]
        |
        | Examples:
        | $CMDLINE -listAddresses -walletId 1234567890123456789012345678901234567890 -state used
        | $CMDLINE -listAddresses -walletId 1234567890123456789012345678901234567890 -state unused
        |
        |-getTx -walletId [walletId] -txId [txId]
        | Get transaction by id
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction ]
        |
        | Examples:
        | $CMDLINE -getTx -walletId 1234567890123456789012345678901234567890 -txId ABCDEF1234567890
        |
        |-createTx -walletId [walletId] -amount [amount] -address [address] -passphrase [passphrase] -metadata [metadata](optional)
        | Create and send transaction from the wallet
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction ]
        |
        | Examples:
        | $CMDLINE -createTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890 -passphrase Password12345!
        | $CMDLINE -createTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890 -passphrase Password12345! -metadata 0:0123456789012345678901234567890123456789012345678901234567890123:2:TESTINGCARDANOAPI
        |
        |-fundTx -walletId [walletId] -amount [amount] -address [address]
        | Select coins to cover the given set of payments
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/selectCoins ]
        |
        | Examples:
        | $CMDLINE -fundTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890
        |
        |-listTxs -walletId [walletId] -start [start_date](optional) -end [end_date](optional) -order [order](optional) -minWithdrawal [minWithdrawal](optional)
        | Lists all incoming and outgoing wallet's transactions, dates in ISO_ZONED_DATE_TIME format, order: ascending, descending ( default )
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listTransactions ]
        |
        | Examples:
        | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890
        | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -start 2020-01-02T10:15:30+01:00
        | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -start 2020-01-02T10:15:30+01:00 -end 2020-09-30T12:00:00+01:00
        | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -order ascending
        | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -minWithdrawal 1
        |
        |-createWallet -name [walletName] -passphrase [passphrase] -mnemonic [mnemonic] -addressPoolGap [address_pool_gap](optional)
        | Create new wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet ]
        |
        | Examples:
        | $CMDLINE -createWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad'
        | $CMDLINE -createWallet -name new_wallet_2 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -addressPoolGap 10
        |
        |-restoreWallet -name [walletName] -passphrase [passphrase] -mnemonic [mnemonic] -addressPoolGap [address_pool_gap](optional)
        | Restore wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet ]
        |
        | Examples:
        | $CMDLINE -restoreWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad''
        | $CMDLINE -restoreWallet -name new_wallet_2 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -addressPoolGap 10
        |""".stripMargin
  }

  private def getUnusedAddressWallet2 = getUnusedAddress(testWallet2Id)

  private def getUnusedAddressWallet1 = getUnusedAddress(testWalletId)

  private def getUnusedAddress(walletId: String): String = {
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

  private def extractTxId(toStringCreateTransactionResult: String): String =
    toStringCreateTransactionResult.split(",").head.stripPrefix("CreateTransactionResponse(")

}
