package iog.psg.cardano

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import io.circe.generic.auto._
import io.circe.parser.{decode, _}
import iog.psg.cardano.CardanoApiCodec.WalletAddressId
import iog.psg.cardano.CardanoApiMain.CmdLine
import iog.psg.cardano.TestWalletsConfig.baseUrl
import iog.psg.cardano.common.TestWalletFixture
import iog.psg.cardano.util.{ArgumentParser, Configure, Trace}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardanoApiMainITSpec extends AnyFlatSpec with Matchers with Configure with ScalaFutures with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    Seq(2, 3).map { num =>
      val walletId = TestWalletsConfig.walletsMap(num).id
      runCmdLine(
        CmdLine.deleteWallet,
        CmdLine.walletId, walletId
      )
    }
    val wallet1 = TestWalletsConfig.walletsMap(1)
    runCmdLine(CmdLine.updateName,
      CmdLine.walletId, wallet1.id,
      CmdLine.name, wallet1.name
    )
    super.afterAll()
  }

  private implicit val system = ActorSystem("SingleRequest")
  private implicit val context = system.dispatcher

  private val defaultArgs = Array(CmdLine.baseUrl, baseUrl)

  private def makeArgs(args: String*): Array[String] =
    defaultArgs ++ args

  private def runCmdLine(args: String*): Seq[String] = {
    val arguments = new ArgumentParser(makeArgs(args: _*))

    var results: Seq[String] = Seq.empty
    implicit val memTrace = new Trace {
      override def apply(s: String): Unit = results = (s: String) +: results
      override def close(): Unit = ()
    }

    implicit val apiRequestExecutor: ApiRequestExecutor = ApiRequestExecutor

    CardanoApiMain.run(arguments)

    results.reverse
  }

  "The Cmd line -netInfo" should "support retrieving network information" in {
    val cmdLineResults = runCmdLine(CmdLine.netInfo)
    assert(cmdLineResults.exists(_.contains("ready")), s"Testnet API service not ready - '$baseUrl' \n $cmdLineResults")
  }

  "The Cmd line -netClockInfo" should "support retrieving network clock information" in {
    val cmdLineResults = runCmdLine(CmdLine.netClockInfo)
    assert(cmdLineResults.exists(_.contains("available")), s"Testnet API service not available - '$baseUrl' \n $cmdLineResults")
  }

  "The Cmd line -netParams" should "support retrieving network params" in {
    val cmdLineResults = runCmdLine(CmdLine.netParams)
    assert(cmdLineResults.exists(_.contains("genesis_block_hash")), s"Testnet API service not available - '$baseUrl' \n $cmdLineResults")
  }

  "The Cmd Line -wallets" should "show our test wallet in the list" in new TestWalletFixture(walletNum = 1) {

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

  "The Cmd Line -estimateFee" should "estimate transaction costs" in new TestWalletFixture(walletNum = 1){
    val unusedAddr = getUnusedAddressWallet1

    val cmdLineResults = runCmdLine(
      CmdLine.estimateFee,
      CmdLine.amount, testAmountToTransfer.get,
      CmdLine.address, unusedAddr,
      CmdLine.walletId, testWalletId)
    assert(cmdLineResults.exists(r => r.contains("estimated_min") && r.contains("estimated_max")))
  }

  it should "estimate transaction costs with metadata" in new TestWalletFixture(walletNum = 1){
    val unusedAddr = getUnusedAddressWallet1

    val cmdLineResults = runCmdLine(
      CmdLine.estimateFee,
      CmdLine.amount, testAmountToTransfer.get,
      CmdLine.address, unusedAddr,
      CmdLine.metadata, testMetadata.get,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(r => r.contains("estimated_min") && r.contains("estimated_max")))
  }

  "The Cmd Line -wallet [walletId]" should "get our wallet" in new TestWalletFixture(walletNum = 1){
    val cmdLineResults = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains(testWalletId)), "Test wallet not found.")
  }

  "The Cmd Line -updateName -wallet [walletId] -name [name]" should "update wallet's name" in new TestWalletFixture(walletNum = 1){
    val newName = s"${testWalletName}_updated"
    val cmdLineResults = runCmdLine(
      CmdLine.updateName,
      CmdLine.walletId, testWalletId,
      CmdLine.name, newName
    )

    assert(cmdLineResults.exists(_.contains(newName)), "wallet's name not updated.")
  }

  "The Cmd Line -createWallet" should "create wallet 2" in new TestWalletFixture(walletNum = 2){
    val results = runCmdLine(
      CmdLine.createWallet,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.name, testWalletName,
      CmdLine.mnemonic, testWalletMnemonic)

    assert(results.last.contains(testWalletId), "Test wallet 2 not found.")
  }

  it should "create wallet with secondary factor" in new TestWalletFixture(walletNum = 3){
    val results = runCmdLine(
      CmdLine.createWallet,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.name, testWalletName,
      CmdLine.mnemonic, testWalletMnemonic,
      CmdLine.mnemonicSecondary, testWalletMnemonicSecondary.get
    )
    assert(results.last.contains(testWalletId), "Test wallet 3 not found.")
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

  "The Cmd Line -updatePassphrase" should "allow password change in test wallet 2" in new TestWalletFixture(walletNum = 2){
    val cmdLineResults = runCmdLine(
      CmdLine.updatePassphrase,
      CmdLine.oldPassphrase, testWalletPassphrase,
      CmdLine.passphrase, testWalletPassphrase.toUpperCase,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains("Unit result from update passphrase")))
  }

  "The Cmd Line -deleteWallet [walletId]" should "delete test wallet 2" in new TestWalletFixture(walletNum = 2){
    val cmdLineResults = runCmdLine(
      CmdLine.deleteWallet,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.exists(_.contains("Unit result from delete wallet")))

    val results = runCmdLine(
      CmdLine.getWallet,
      CmdLine.walletId, testWalletId)

    assert(results.exists(!_.contains(testWalletId)), "Test wallet found after deletion?")
  }

  "The Cmd Line -restoreWallet" should "restore deleted wallet 2" in new TestWalletFixture(walletNum = 2){
    val cmdLineResults = runCmdLine(
      CmdLine.restoreWallet,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.name, testWalletName,
      CmdLine.mnemonic, testWalletMnemonic)

    val jsonResponse = parse(cmdLineResults.last).getOrElse(fail("Invalid json"))
    val id = jsonResponse.\\("id").headOption.flatMap(_.asString).getOrElse(fail("Missing id field"))

    id shouldBe testWalletId
  }

  "The Cmd Line -listAddresses -walletId [walletId] -state [state]" should "list unused wallet addresses" in new TestWalletFixture(walletNum = 1){
    val cmdLineResults = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "unused",
      CmdLine.walletId, testWalletId)

    val jsonResponse = parse(cmdLineResults.last).getOrElse(fail("Invalid json"))
    val states = jsonResponse.\\("state").flatMap(_.asString)

    assert(states.contains("unused"))
    assert(!states.contains("used"))
  }

  it should "list used wallet addresses" in new TestWalletFixture(walletNum = 1){
    val cmdLineResults = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "used",
      CmdLine.walletId, testWalletId)

    val jsonResponse = parse(cmdLineResults.last).getOrElse(fail("Invalid json"))
    val states = jsonResponse.\\("state").flatMap(_.asString)

    assert(!states.contains("unused"))
    assert(states.contains("used"))
  }

  "The Cmd Line -fundTx" should "fund payments" in new TestWalletFixture(walletNum = 1){
    val cmdLineResults = runCmdLine(
      CmdLine.fundTx,
      CmdLine.amount, testAmountToTransfer.get,
      CmdLine.address, getUnusedAddressWallet2,
      CmdLine.walletId, testWalletId)

    assert(cmdLineResults.last.contains("FundPaymentsResponse") ||
      cmdLineResults.mkString("").contains("cannot_cover_fee"), s"$cmdLineResults")
  }

  "The Cmd Lines -createTx, -getTx, -listTxs" should "transact from A to B with metadata, txId should be visible in get and list" in new TestWalletFixture(walletNum = 1){
    val unusedAddr = getUnusedAddressWallet1
    val preTxTime = ZonedDateTime.now().minusMinutes(1)

    val resultsCreateTx = runCmdLine(
      CmdLine.createTx,
      CmdLine.passphrase, testWalletPassphrase,
      CmdLine.amount, testAmountToTransfer.get,
      CmdLine.metadata, testMetadata.get,
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

    val resultDelete = runCmdLine(
      CmdLine.deleteTx,
      CmdLine.txId, txId,
      CmdLine.walletId, testWalletId
    )

    val resultsListWalletTxsAfterDelete = runCmdLine(
      CmdLine.listWalletTransactions,
      CmdLine.start, preTxTime.toString,
      CmdLine.`end`, postTxTime.toString,
      CmdLine.walletId, testWalletId)

    val notFoundTx = !resultsListWalletTxsAfterDelete.exists(_.contains(txId))
    assert(notFoundTx, s"txId $txId not deleted")
  }

  "The Cmd Lines -inspectAddress" should "inspect address" in new TestWalletFixture(walletNum = 1){
    val unusedAddr = getUnusedAddressWallet1
    val results = runCmdLine(
      CmdLine.inspectWalletAddress,
      CmdLine.address, unusedAddr
    )
    assert(results.exists(_.contains("address_style")), "missing address_style")
  }

  "The Cmd Lines -getUTxO" should "get UTxOs statistics" in new TestWalletFixture(walletNum = 1){
    val results = runCmdLine(
      CmdLine.getUTxOsStatistics,
      CmdLine.walletId, testWalletId
    )
    assert(results.exists(_.contains("distribution")), "Missing UTxOs distribution across the whole wallet")
  }

  "The Cmd Line --help" should "show possible commands" in {
    val results = runCmdLine(CmdLine.help)
    results.mkString("\n") shouldBe
      """This super simple tool allows developers to access a cardano wallet backend from the command line
        |
        |Usage:
        |
        | export CMDLINE='java -jar psg-cardano-wallet-api-assembly-<VER>.jar'
        | $CMDLINE <command> <arguments>
        |
        |Optional:
        |
        | -baseUrl <url> <command>
        | -trace <filename> <command>
        | -noConsole <command>
        |
        |Commands:
        |
        | -netInfo
        | -netClockInfo
        | -netParams
        | -wallets
        | -deleteWallet -walletId <walletId>
        | -wallet -walletId <walletId>
        | -updateName -walletId <walletId> -name <name>
        | -createWallet -name <walletName> -passphrase <passphrase> -mnemonic <mnemonic> [-mnemonicSecondary <mnemonicSecondary>] [-addressPoolGap <mnemonicaddress_pool_gap>]
        | -restoreWallet -name <walletName> -passphrase <passphrase> -mnemonic <mnemonic> [-mnemonicSecondary <mnemonicSecondary>] [-addressPoolGap <mnemonicaddress_pool_gap>]
        | -estimateFee -walletId <walletId> -amount <amount> -address <address>
        | -updatePassphrase -walletId <walletId> -oldPassphrase <oldPassphrase> -passphrase <newPassphrase>
        | -listAddresses -walletId <walletId> -state <state>
        | -inspectAddress -address <address>
        | -listTxs -walletId <walletId> [-start <start_date>] [-end <end_date>] [-order <order>] [-minWithdrawal <minWithdrawal>]
        | -createTx -walletId <walletId> -amount <amount> -address <address> -passphrase <passphrase> [-metadata <metadata>]
        | -deleteTx -walletId <walletId> -txId <txId>
        | -fundTx -walletId <walletId> -amount <amount> -address <address>
        | -getTx -walletId <walletId> -txId <txId>
        | -getUTxO -walletId <walletId>""".stripMargin
  }

  it should "show -baseUrl help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.baseUrl)

    results.mkString("\n").stripMargin.trim shouldBe """ define different api url ( default : http://127.0.0.1:8090/v2/ )
                                                       |
                                                       | Arguments: <url> <command>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -baseUrl http://cardano-wallet-testnet.mydomain:8090/v2/ -wallets""".stripMargin.trim
  }

  it should "show -trace help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.traceToFile)
    results.mkString("\n").stripMargin.trim shouldBe """ write logs into a defined file ( default file name: cardano-api.log )
                                                       |
                                                       | Arguments: <filename> <command>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -trace wallets.log -wallets""".stripMargin.trim
  }

  it should "show -noConsole help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.noConsole)
    results.mkString("\n").stripMargin.trim shouldBe """ run a command without any logging
                                                       |
                                                       | Arguments: <command>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -noConsole -deleteWallet -walletId 1234567890123456789012345678901234567890""".stripMargin.trim
  }

  it should "show -netInfo help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.netInfo)
    results.mkString("\n").stripMargin.trim shouldBe """ Show network information
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkInformation ]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -netInfo""".stripMargin.trim
  }

  it should "show -netClockInfo help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.netClockInfo)
    results.mkString("\n").stripMargin.trim shouldBe """ Show network clock information
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkClock ]
                                                       |
                                                       | Arguments: [-forceNtpCheck <forceNtpCheck>]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -netClockInfo
                                                       | $CMDLINE -netClockInfo -forceNtpCheck true""".stripMargin.trim
  }

  it should "show -netParams help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.netParams)
    results.mkString("\n").stripMargin.trim shouldBe """ Returns the set of network parameters for the current epoch.
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getNetworkParameters ]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -netParams""".stripMargin.trim
  }

  it should "show listWallets help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.listWallets)
    results.mkString("\n").stripMargin.trim shouldBe """ Return a list of known wallets, ordered from oldest to newest
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listWallets ]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -wallets""".stripMargin.trim
  }

  it should "show deleteWallet help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.deleteWallet)
    results.mkString("\n").stripMargin.trim shouldBe """ Delete wallet by id
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteWallet ]
                                                       |
                                                       | Arguments: -walletId <walletId>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -deleteWallet -walletId 1234567890123456789012345678901234567890""".stripMargin.trim
  }

  it should "show getWallet help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.getWallet)
    results.mkString("\n").stripMargin.trim shouldBe """ Get wallet by id
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet ]
                                                       |
                                                       | Arguments: -walletId <walletId>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -wallet -walletId 1234567890123456789012345678901234567890""".stripMargin.trim
  }

  it should "show updateName help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.updateName)
    results.mkString("\n").stripMargin.trim shouldBe """ Update wallet's name
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWallet ]
                                                       |
                                                       | Arguments: -walletId <walletId> -name <name>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -updateName -walletId 1234567890123456789012345678901234567890 -name new_name""".stripMargin.trim
  }

  it should "show createWallet help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.createWallet)
    results.mkString("\n").stripMargin.trim shouldBe """ Create new wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet ]
                                                       |
                                                       | Arguments: -name <walletName> -passphrase <passphrase> -mnemonic <mnemonic> [-mnemonicSecondary <mnemonicSecondary>] [-addressPoolGap <mnemonicaddress_pool_gap>]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -createWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad'
                                                       | $CMDLINE -createWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -mnemonicSecondary 'ability make always any pulse swallow marriage media dismiss'
                                                       | $CMDLINE -createWallet -name new_wallet_2 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -addressPoolGap 10""".stripMargin.trim
  }

  it should "show restoreWallet help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.restoreWallet)
    results.mkString("\n").stripMargin.trim shouldBe """ Restore wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postWallet ]
                                                       |
                                                       | Arguments: -name <walletName> -passphrase <passphrase> -mnemonic <mnemonic> [-mnemonicSecondary <mnemonicSecondary>] [-addressPoolGap <mnemonicaddress_pool_gap>]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -restoreWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad'
                                                       | $CMDLINE -restoreWallet -name new_wallet_1 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -mnemonicSecondary 'ability make always any pulse swallow marriage media dismiss'
                                                       | $CMDLINE -restoreWallet -name new_wallet_2 -passphrase Password12345! -mnemonic 'ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad' -addressPoolGap 10
                                                       |""".stripMargin.trim
  }

  it should "show estimateFee help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.estimateFee)
    results.mkString("\n").stripMargin.trim shouldBe """ Estimate fee for the transaction
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee ]
                                                       |
                                                       | Arguments: -walletId <walletId> -amount <amount> -address <address>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -estimateFee -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890
                                                       |""".stripMargin.trim
  }

  it should "show updatePassphrase help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.updatePassphrase)
    results.mkString("\n").stripMargin.trim shouldBe """ Update passphrase
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/putWalletPassphrase ]
                                                       |
                                                       | Arguments: -walletId <walletId> -oldPassphrase <oldPassphrase> -passphrase <newPassphrase>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -updatePassphrase -walletId 1234567890123456789012345678901234567890 -oldPassphrase OldPassword12345! -passphrase NewPassword12345!""".stripMargin.trim
  }

  it should "show listWalletAddresses help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.listWalletAddresses)
    results.mkString("\n").stripMargin.trim shouldBe """ Return a list of known addresses, ordered from newest to oldest, state: used, unused
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listAddresses ]
                                                       |
                                                       | Arguments: -walletId <walletId> -state <state>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -listAddresses -walletId 1234567890123456789012345678901234567890 -state used
                                                       | $CMDLINE -listAddresses -walletId 1234567890123456789012345678901234567890 -state unused""".stripMargin.trim
  }

  it should "show inspectWalletAddress help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.inspectWalletAddress)
    results.mkString("\n").stripMargin.trim shouldBe """ Give useful information about the structure of a given address.
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/inspectAddress ]
                                                       |
                                                       | Arguments: -address <address>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -inspectAddress -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890""".stripMargin.trim
  }

  it should "show listWalletTransactions help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.listWalletTransactions)
    results.mkString("\n").stripMargin.trim shouldBe """ Lists all incoming and outgoing wallet's transactions, dates in ISO_ZONED_DATE_TIME format, order: ascending, descending ( default )
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/listTransactions ]
                                                       |
                                                       | Arguments: -walletId <walletId> [-start <start_date>] [-end <end_date>] [-order <order>] [-minWithdrawal <minWithdrawal>]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890
                                                       | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -start 2020-01-02T10:15:30+01:00
                                                       | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -start 2020-01-02T10:15:30+01:00 -end 2020-09-30T12:00:00+01:00
                                                       | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -order ascending
                                                       | $CMDLINE -listTxs -walletId 1234567890123456789012345678901234567890 -minWithdrawal 1""".stripMargin.trim
  }

  it should "show createTx help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.createTx)
    results.mkString("\n").stripMargin.trim shouldBe """ Create and send transaction from the wallet
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction ]
                                                       |
                                                       | Arguments: -walletId <walletId> -amount <amount> -address <address> -passphrase <passphrase> [-metadata <metadata>]
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -createTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890 -passphrase Password12345!
                                                       | $CMDLINE -createTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890 -passphrase Password12345! -metadata 0:0123456789012345678901234567890123456789012345678901234567890123:2:TESTINGCARDANOAPI
                                                       |""".stripMargin.trim
  }

  it should "show fundTx help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.fundTx)
    results.mkString("\n").stripMargin.trim shouldBe """ Select coins to cover the given set of payments
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/selectCoins ]
                                                       |
                                                       | Arguments: -walletId <walletId> -amount <amount> -address <address>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -fundTx -walletId 1234567890123456789012345678901234567890 -amount 20000 -address addr12345678901234567890123456789012345678901234567890123456789012345678901234567890
                                                       | """.stripMargin.trim
  }

  it should "show deleteTx help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.deleteTx)
    results.mkString("\n").stripMargin.trim shouldBe """ Delete pending transaction by id
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteTransaction ]
                                                       |
                                                       | Arguments: -walletId <walletId> -txId <txId>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -deleteTx -walletId 1234567890123456789012345678901234567890 -txId ABCDEF1234567890""".stripMargin.trim
  }

  it should "show getTx help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.getTx)
    results.mkString("\n").stripMargin.trim shouldBe """ Get transaction by id
                                                       | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction ]
                                                       |
                                                       | Arguments: -walletId <walletId> -txId <txId>
                                                       |
                                                       | Examples:
                                                       | $CMDLINE -getTx -walletId 1234567890123456789012345678901234567890 -txId ABCDEF1234567890""".stripMargin.trim
  }

  it should "show getUTxO help" in {
    val results = runCmdLine(CmdLine.help, CmdLine.getUTxOsStatistics)
    results.mkString("\n").stripMargin.trim shouldBe
      """ Return the UTxOs distribution across the whole wallet, in the form of a histogram
        | [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getUTxOsStatistics ]
        |
        | Arguments: -walletId <walletId>
        |
        | Examples:
        | $CMDLINE -getUTxO -walletId 1234567890123456789012345678901234567890""".stripMargin.trim
  }

  private def getUnusedAddressWallet2 = getUnusedAddress(TestWalletsConfig.walletsMap(2).id)

  private def getUnusedAddressWallet1 = getUnusedAddress(TestWalletsConfig.walletsMap(1).id)

  private def getUnusedAddress(walletId: String): String = {
    val cmdResults: Seq[String] = runCmdLine(
      CmdLine.listWalletAddresses,
      CmdLine.state, "unused",
      CmdLine.walletId, walletId)

    decode[Seq[WalletAddressId]](cmdResults.last).toOption.getOrElse(Nil).last.id
  }

  private def extractTxId(toStringCreateTransactionResult: String): String = {
    val json = parse(toStringCreateTransactionResult).getOrElse(fail("Invalid json"))
    json.\\("id").headOption.flatMap(_.asString).getOrElse(fail(s"Could not parse id from json: ${toStringCreateTransactionResult}"))
  }

}
