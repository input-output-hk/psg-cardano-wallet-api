package iog.psg.cardano

import java.io.File
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps.{CardanoApiRequestFOps, CardanoApiRequestOps}
import iog.psg.cardano.CardanoApi._
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.util.StringToMetaMapParser.toMetaMap
import iog.psg.cardano.util._

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object CardanoApiMain {

  object CmdLine {
    val help = "-help"
    val traceToFile = "-trace"
    val noConsole = "-noConsole"
    val netInfo = "-netInfo"
    val netClockInfo = "-netClockInfo"
    val baseUrl = "-baseUrl"
    val listWallets = "-wallets"
    val updateName = "-updateName"
    val deleteWallet = "-deleteWallet"
    val getWallet = "-wallet"
    val createWallet = "-createWallet"
    val restoreWallet = "-restoreWallet"
    val estimateFee = "-estimateFee"
    val name = "-name"
    val updatePassphrase = "-updatePassphrase"
    val oldPassphrase = "-oldPassphrase"
    val passphrase = "-passphrase"
    val metadata = "-metadata"
    val mnemonic = "-mnemonic"
    val mnemonicSecondary = "-mnemonicSecondary"
    val addressPoolGap = "-addressPoolGap"
    val listWalletAddresses = "-listAddresses"
    val inspectWalletAddress = "-inspectAddress"
    val listWalletTransactions = "-listTxs"
    val state = "-state"
    val walletId = "-walletId"
    val start = "-start"
    val end = "-end"
    val order = "-order"
    val minWithdrawal = "-minWithdrawal"
    val createTx = "-createTx"
    val fundTx = "-fundTx"
    val getTx = "-getTx"
    val deleteTx = "-deleteTx"
    val txId = "-txId"
    val amount = "-amount"
    val address = "-address"
    val getUTxOsStatistics = "-getUTxO"
  }

  val defaultBaseUrl = "http://127.0.0.1:8090/v2/"
  val defaultTraceFile = "cardano-api.log"

  def main(args: Array[String]): Unit = {

    val arguments = new ArgumentParser(args)
    val helpMode = arguments.contains(CmdLine.help)

    implicit val trace = if (helpMode) {
      ConsoleTrace
    } else {
      val conTracer = if (arguments.contains(CmdLine.noConsole)) NoOpTrace else ConsoleTrace
      conTracer.withTrace(
        if (arguments.contains(CmdLine.traceToFile)) {
          val fileName = arguments(CmdLine.traceToFile).getOrElse(defaultTraceFile)
          new FileTrace(new File(fileName))
        } else NoOpTrace
      )
    }

    implicit val apiRequestExecutor: ApiRequestExecutor = ApiRequestExecutor

    run(arguments)
  }

  private[cardano] def run(arguments: ArgumentParser)(implicit trace: Trace, apiRequestExecutor: ApiRequestExecutor): Unit = {

    if (arguments.noArgs || arguments.contains(CmdLine.help)) {
      showHelp(arguments.params.filterNot(_ == CmdLine.help))
    } else {

      def hasArgument(arg: String): Boolean = {
        val result = arguments.contains(arg)
        if (result) trace(arg)
        result
      }

      implicit val system: ActorSystem = ActorSystem("SingleRequest")
      import system.dispatcher

      Try {

        val url = arguments(CmdLine.baseUrl).getOrElse(defaultBaseUrl)

        trace(s"baseurl:$url")

        val api = new CardanoApi(url)

        if (hasArgument(CmdLine.netInfo)) {
          unwrap[CardanoApiCodec.NetworkInfo](api.networkInfo.executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.netClockInfo)) {
          unwrap[CardanoApiCodec.NetworkClock](api.networkClock.executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.listWallets)) {
          unwrap[Seq[CardanoApiCodec.Wallet]](api.listWallets.executeBlocking, r => r.foreach(trace(_)))
        } else if (hasArgument(CmdLine.estimateFee)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))
          unwrap[CardanoApiCodec.EstimateFeeResponse](api.estimateFee(walletId, payments, None).executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.getWallet)) {
          val walletId = arguments.get(CmdLine.walletId)
          unwrap[CardanoApiCodec.Wallet](api.getWallet(walletId).executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.updatePassphrase)) {
          val walletId = arguments.get(CmdLine.walletId)
          val oldPassphrase = arguments.get(CmdLine.oldPassphrase)
          val newPassphrase = arguments.get(CmdLine.passphrase)

          unwrap[Unit](api.updatePassphrase(walletId, oldPassphrase, newPassphrase).executeBlocking, _ => trace("Unit result from update passphrase"))

        } else if (hasArgument(CmdLine.updateName)) {
          val walletId = arguments.get(CmdLine.walletId)
          val name = arguments.get(CmdLine.name)
          unwrap[CardanoApiCodec.Wallet](api.updateName(walletId, name).executeBlocking,trace(_))
        } else if (hasArgument(CmdLine.deleteWallet)) {
          val walletId = arguments.get(CmdLine.walletId)
          unwrap[Unit](api.deleteWallet(walletId).executeBlocking, _ => trace("Unit result from delete wallet"))
        } else if (hasArgument(CmdLine.listWalletAddresses)) {
          val walletId = arguments.get(CmdLine.walletId)
          val addressesState = Some(AddressFilter.withName(arguments.get(CmdLine.state)))
          unwrap[Seq[CardanoApiCodec.WalletAddressId]](api.listAddresses(walletId, addressesState).executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.inspectWalletAddress)) {
          val address = arguments.get(CmdLine.address)
          unwrap[WalletAddress](api.inspectAddress(address).executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.getTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val txId = arguments.get(CmdLine.txId)
          unwrap[CardanoApiCodec.CreateTransactionResponse](api.getTransaction(walletId, txId).executeBlocking, trace(_))
        } else if (hasArgument(CmdLine.deleteTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val txId = arguments.get(CmdLine.txId)
          unwrap[Unit](api.deleteTransaction(walletId, txId).executeBlocking, _ => trace("Unit result from delete transaction"))
        } else if (hasArgument(CmdLine.createTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val pass = arguments.get(CmdLine.passphrase)
          val metadata = toMetaMap(arguments(CmdLine.metadata))
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))

          unwrap[CardanoApiCodec.CreateTransactionResponse](api.createTransaction(
            walletId,
            pass,
            payments,
            metadata,
            None
          ).executeBlocking, trace(_))

        } else if (hasArgument(CmdLine.fundTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))

          unwrap[CardanoApiCodec.FundPaymentsResponse](api.fundPayments(
            walletId,
            payments
          ).executeBlocking, r => trace(r.toString))

        } else if (hasArgument(CmdLine.listWalletTransactions)) {
          val walletId = arguments.get(CmdLine.walletId)
          val startDate = arguments(CmdLine.start).map(strToZonedDateTime)
          val endDate = arguments(CmdLine.end).map(strToZonedDateTime)
          val orderOf = arguments(CmdLine.order).flatMap(s => Try(Order.withName(s)).toOption).getOrElse(Order.descendingOrder)
          val minWithdrawalTx = arguments(CmdLine.minWithdrawal).map(_.toInt)

          unwrap[Seq[CardanoApiCodec.CreateTransactionResponse]](api.listTransactions(
            walletId,
            startDate,
            endDate,
            orderOf,
            minWithdrawal = minWithdrawalTx
          ).executeBlocking, r => if (r.isEmpty) trace("No txs returned") else r.foreach(trace(_)))

        } else if (hasArgument(CmdLine.createWallet) || hasArgument(CmdLine.restoreWallet)) {
          val name = arguments.get(CmdLine.name)
          val passphrase = arguments.get(CmdLine.passphrase)
          val mnemonic = arguments.get(CmdLine.mnemonic)
          val mnemonicSecondaryOpt = arguments(CmdLine.mnemonicSecondary)
          val addressPoolGap = arguments(CmdLine.addressPoolGap).map(_.toInt)

          unwrap[CardanoApiCodec.Wallet](api.createRestoreWallet(
            name,
            passphrase,
            GenericMnemonicSentence(mnemonic),
            mnemonicSecondaryOpt.map(m => GenericMnemonicSecondaryFactor(m)),
            addressPoolGap
          ).executeBlocking, trace(_))

        } else if (hasArgument(CmdLine.getUTxOsStatistics)) {
          val walletId = arguments.get(CmdLine.walletId)
          unwrap[UTxOStatistics](api.getUTxOsStatistics(walletId).executeBlocking, trace(_))
        } else {
          trace("No command recognised")
        }

      }.recover {
        case e => trace(e.toString)
      }
      trace.close()
      system.terminate()

    }
  }


  private def strToZonedDateTime(dtStr: String): ZonedDateTime = {
    ZonedDateTime.parse(dtStr)
  }

  private def showHelp(extraParams: List[String])(implicit trace: Trace): Unit = {
    val exampleWalletId = "1234567890123456789012345678901234567890"
    val exampleTxd = "ABCDEF1234567890"
    val exampleAddress = "addr12345678901234567890123456789012345678901234567890123456789012345678901234567890"
    val exampleMetadata = "0:0123456789012345678901234567890123456789012345678901234567890123:2:TESTINGCARDANOAPI"
    val exampleMnemonic = "ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad"
    val exampleMnemonicSecondary = "ability make always any pulse swallow marriage media dismiss"

    def beautifyTrace(arguments: String, description: String, examples: List[String], apiDocOperation: String = ""): Unit = {
      val docsUrl = if (apiDocOperation.nonEmpty) s" [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/$apiDocOperation ]\n" else ""
      val examplesStr = s" Examples:\n ${examples.map("$CMDLINE "+_).mkString("\n ")}"
      val argumentsLine = if (arguments.nonEmpty) s" Arguments: $arguments\n\n" else ""
      trace(s"\n $description\n$docsUrl\n$argumentsLine$examplesStr\n")
    }

    val cmdLineNetInfo = s"${CmdLine.netInfo}"
    val cmdLineNetClockInfo = s"${CmdLine.netClockInfo}"
    val cmdLineListWallets = s"${CmdLine.listWallets}"
    val cmdLineEstimateFee = s"${CmdLine.estimateFee} ${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address>"
    val cmdLineGetWallet = s"${CmdLine.getWallet} ${CmdLine.walletId} <walletId>"
    val cmdLineUpdateName = s"${CmdLine.updateName} ${CmdLine.walletId} <walletId> ${CmdLine.name} <name>"
    val cmdLineUpdatePassphrase = s"${CmdLine.updatePassphrase} ${CmdLine.walletId} <walletId> ${CmdLine.oldPassphrase} <oldPassphrase> ${CmdLine.passphrase} <newPassphrase>"
    val cmdLineDeleteWallet = s"${CmdLine.deleteWallet} ${CmdLine.walletId} <walletId>"
    val cmdLineListWalletAddresses = s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} <walletId> ${CmdLine.state} <state>"
    val cmdLineInspectWalletAddress = s"${CmdLine.inspectWalletAddress} ${CmdLine.address} <address>"
    val cmdLineGetTx = s"${CmdLine.getTx} ${CmdLine.walletId} <walletId> ${CmdLine.txId} <txId>"
    val cmdLineCreateTx = s"${CmdLine.createTx} ${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address> ${CmdLine.passphrase} <passphrase> [${CmdLine.metadata} <metadata>]"
    val cmdLineDeleteTx = s"${CmdLine.deleteTx} ${CmdLine.walletId} <walletId> ${CmdLine.txId} <txId>"
    val cmdLineFundTx = s"${CmdLine.fundTx} ${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address>"
    val cmdLineListWalletTransactions = s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} <walletId> [${CmdLine.start} <start_date>] [${CmdLine.end} <end_date>] [${CmdLine.order} <order>] [${CmdLine.minWithdrawal} <minWithdrawal>]"
    val cmdLineCreateWallet = s"${CmdLine.createWallet} ${CmdLine.name} <walletName> ${CmdLine.passphrase} <passphrase> ${CmdLine.mnemonic} <mnemonic> [${CmdLine.mnemonicSecondary} <mnemonicSecondary>] [${CmdLine.addressPoolGap} <mnemonicaddress_pool_gap>]"
    val cmdLineRestoreWallet = s"${CmdLine.restoreWallet} ${CmdLine.name} <walletName> ${CmdLine.passphrase} <passphrase> ${CmdLine.mnemonic} <mnemonic> [${CmdLine.mnemonicSecondary} <mnemonicSecondary>] [${CmdLine.addressPoolGap} <mnemonicaddress_pool_gap>]"
    val cmdLineGetUTxOsStatistics = s"${CmdLine.getUTxOsStatistics} ${CmdLine.walletId} <walletId>"

    val cmdLineBaseUrl = s"${CmdLine.baseUrl} <url> <command>"
    val cmdLineTraceToFile = s"${CmdLine.traceToFile} <filename> <command>"
    val cmdLineNoConsole = s"${CmdLine.noConsole} <command>"

    if (extraParams.isEmpty) {
      trace("This super simple tool allows developers to access a cardano wallet backend from the command line\n")
      trace("Usage:\n")
      trace(" export CMDLINE='java -jar psg-cardano-wallet-api-assembly-<VER>.jar'")
      trace(" $CMDLINE <command> <arguments>\n")

      trace("Optional:\n")
      trace(" "+cmdLineBaseUrl)
      trace(" "+cmdLineTraceToFile)
      trace(" "+cmdLineNoConsole)

      trace("\nCommands:\n")
      trace(" "+cmdLineNetInfo)
      trace(" "+cmdLineNetClockInfo)
      trace(" "+cmdLineListWallets)
      trace(" "+cmdLineDeleteWallet)
      trace(" "+cmdLineGetWallet)
      trace(" "+cmdLineUpdateName)
      trace(" "+cmdLineCreateWallet)
      trace(" "+cmdLineRestoreWallet)
      trace(" "+cmdLineEstimateFee)
      trace(" "+cmdLineUpdatePassphrase)
      trace(" "+cmdLineListWalletAddresses)
      trace(" "+cmdLineInspectWalletAddress)
      trace(" "+cmdLineListWalletTransactions)
      trace(" "+cmdLineCreateTx)
      trace(" "+cmdLineDeleteTx)
      trace(" "+cmdLineFundTx)
      trace(" "+cmdLineGetTx)
      trace(" "+cmdLineGetUTxOsStatistics)
    } else {
      extraParams.headOption.getOrElse("") match {
        case CmdLine.baseUrl =>
          beautifyTrace(
            arguments = "<url> <command>",
            description = s"define different api url ( default : ${CardanoApiMain.defaultBaseUrl} )",
            examples = List(
              s"${CmdLine.baseUrl} http://cardano-wallet-testnet.mydomain:8090/v2/ ${CmdLine.listWallets}"
            )
          )
        case CmdLine.traceToFile =>
          beautifyTrace(
            arguments = "<filename> <command>",
            description = s"write logs into a defined file ( default file name: ${CardanoApiMain.defaultTraceFile} )",
            examples = List(
              s"${CmdLine.traceToFile} wallets.log ${CmdLine.listWallets}"
            )
          )
        case CmdLine.noConsole =>
          beautifyTrace(
            arguments = "<command>",
            description = "run a command without any logging",
            examples = List(
              s"${CmdLine.noConsole} ${CmdLine.deleteWallet} ${CmdLine.walletId} $exampleWalletId"
            )
          )
        case CmdLine.netInfo =>
          beautifyTrace(
            arguments = "",
            description = "Show network information",
            apiDocOperation = "getNetworkInformation",
            examples = List(
              s"${CmdLine.netInfo}"
            )
          )
        case CmdLine.netClockInfo =>
          beautifyTrace(
            arguments = "",
            description = "Show network clock information",
            apiDocOperation = "getNetworkClock",
            examples = List(
              s"${CmdLine.netClockInfo}"
            )
          )
        case CmdLine.listWallets =>
          beautifyTrace(
            arguments = "",
            description = "Return a list of known wallets, ordered from oldest to newest",
            apiDocOperation = "listWallets",
            examples = List(
              s"${CmdLine.listWallets}"
            )
          )
        case CmdLine.deleteWallet =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId>",
            description = "Delete wallet by id",
            apiDocOperation = "deleteWallet",
            examples = List(
              s"${CmdLine.deleteWallet} ${CmdLine.walletId} $exampleWalletId"
            )
          )
        case CmdLine.getWallet =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId>",
            description = "Get wallet by id",
            apiDocOperation = "getWallet",
            examples = List(
              s"${CmdLine.getWallet} ${CmdLine.walletId} $exampleWalletId"
            )
          )
        case CmdLine.updateName =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.name} <name>",
            description = "Update wallet's name",
            apiDocOperation = "putWallet",
            examples = List(
              s"${CmdLine.updateName} ${CmdLine.walletId} $exampleWalletId ${CmdLine.name} new_name"
            )
          )
        case CmdLine.createWallet =>
          beautifyTrace(
            arguments = s"${CmdLine.name} <walletName> ${CmdLine.passphrase} <passphrase> ${CmdLine.mnemonic} <mnemonic> [${CmdLine.mnemonicSecondary} <mnemonicSecondary>] [${CmdLine.addressPoolGap} <mnemonicaddress_pool_gap>]",
            description = "Create new wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )",
            apiDocOperation = "postWallet",
            examples = List(
              s"${CmdLine.createWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic'",
              s"${CmdLine.createWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.mnemonicSecondary} '$exampleMnemonicSecondary'",
              s"${CmdLine.createWallet} ${CmdLine.name} new_wallet_2 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.addressPoolGap} 10"
            )
          )
        case CmdLine.restoreWallet =>
          beautifyTrace(
            arguments = s"${CmdLine.name} <walletName> ${CmdLine.passphrase} <passphrase> ${CmdLine.mnemonic} <mnemonic> [${CmdLine.mnemonicSecondary} <mnemonicSecondary>] [${CmdLine.addressPoolGap} <mnemonicaddress_pool_gap>]",
            description = "Restore wallet ( mnemonic can be generated on: https://iancoleman.io/bip39/ )",
            apiDocOperation = "postWallet",
            examples = List(
              s"${CmdLine.restoreWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic'",
              s"${CmdLine.restoreWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.mnemonicSecondary} '$exampleMnemonicSecondary'",
              s"${CmdLine.restoreWallet} ${CmdLine.name} new_wallet_2 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.addressPoolGap} 10"
            )
          )
        case CmdLine.estimateFee =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address>",
            description = "Estimate fee for the transaction",
            apiDocOperation = "postTransactionFee",
            examples = List(
              s"${CmdLine.estimateFee} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress"
            )
          )
        case CmdLine.updatePassphrase =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.oldPassphrase} <oldPassphrase> ${CmdLine.passphrase} <newPassphrase>",
            description = "Update passphrase",
            apiDocOperation = "putWalletPassphrase",
            examples = List(
              s"${CmdLine.updatePassphrase} ${CmdLine.walletId} $exampleWalletId ${CmdLine.oldPassphrase} OldPassword12345! ${CmdLine.passphrase} NewPassword12345!"
            )
          )
        case CmdLine.listWalletAddresses =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.state} <state>",
            description = "Return a list of known addresses, ordered from newest to oldest, state: used, unused",
            apiDocOperation = "listAddresses",
            examples = List(
              s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} $exampleWalletId ${CmdLine.state} ${AddressFilter.used}",
              s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} $exampleWalletId ${CmdLine.state} ${AddressFilter.unUsed}"
            )
          )
        case CmdLine.inspectWalletAddress =>
          beautifyTrace(
            arguments = s"${CmdLine.address} <address>",
            description = "Give useful information about the structure of a given address.",
            apiDocOperation = "inspectAddress",
            examples = List(
              s"${CmdLine.inspectWalletAddress} ${CmdLine.address} $exampleAddress"
            )
          )
        case CmdLine.listWalletTransactions =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> [${CmdLine.start} <start_date>] [${CmdLine.end} <end_date>] [${CmdLine.order} <order>] [${CmdLine.minWithdrawal} <minWithdrawal>]",
            description = "Lists all incoming and outgoing wallet's transactions, dates in ISO_ZONED_DATE_TIME format, order: ascending, descending ( default )",
            apiDocOperation = "listTransactions",
            examples = List(
              s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} $exampleWalletId",
              s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} $exampleWalletId ${CmdLine.start} 2020-01-02T10:15:30+01:00",
              s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} $exampleWalletId ${CmdLine.start} 2020-01-02T10:15:30+01:00 ${CmdLine.end} 2020-09-30T12:00:00+01:00",
              s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} $exampleWalletId ${CmdLine.order} ${Order.ascendingOrder}",
              s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} $exampleWalletId ${CmdLine.minWithdrawal} 1"
            )
          )
        case CmdLine.createTx =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address> ${CmdLine.passphrase} <passphrase> [${CmdLine.metadata} <metadata>]",
            description = "Create and send transaction from the wallet",
            apiDocOperation = "postTransaction",
            examples = List(
              s"${CmdLine.createTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress ${CmdLine.passphrase} Password12345!",
              s"${CmdLine.createTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress ${CmdLine.passphrase} Password12345! ${CmdLine.metadata} $exampleMetadata",
            )
          )
        case CmdLine.fundTx =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.amount} <amount> ${CmdLine.address} <address>",
            description = "Select coins to cover the given set of payments",
            apiDocOperation = "selectCoins",
            examples = List(
              s"${CmdLine.fundTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress"
            )
          )
        case CmdLine.getTx =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.txId} <txId>",
            description = "Get transaction by id",
            apiDocOperation = "getTransaction",
            examples = List(
              s"${CmdLine.getTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.txId} $exampleTxd"
            )
          )
        case CmdLine.deleteTx =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId> ${CmdLine.txId} <txId>",
            description = "Delete pending transaction by id",
            apiDocOperation = "deleteTransaction",
            examples = List(
              s"${CmdLine.deleteTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.txId} $exampleTxd"
            )
          )
        case CmdLine.getUTxOsStatistics =>
          beautifyTrace(
            arguments = s"${CmdLine.walletId} <walletId>",
            description = "Return the UTxOs distribution across the whole wallet, in the form of a histogram",
            apiDocOperation = "getUTxOsStatistics",
            examples = List(
              s"${CmdLine.getUTxOsStatistics} ${CmdLine.walletId} $exampleWalletId"
            )
          )
        case cmd => trace(s"$cmd help not supported")
      }
    }
  }

  def unwrap[T: ClassTag](apiResult: CardanoApiResponse[T], onSuccess: T => Unit)(implicit t: Trace): Unit =
    unwrapOpt(Try(apiResult)).foreach(onSuccess)

  def unwrapOpt[T: ClassTag](apiResult: Try[CardanoApiResponse[T]])(implicit trace: Trace): Option[T] = apiResult match {
    case Success(Left(ErrorMessage(message, code))) =>
      trace(s"API Error message $message, code $code")
      None
    case Success(Right(t: T)) => Some(t)
    case Failure(exception) =>
      println(exception)
      None
  }

  def fail[T](msg: String): T = throw new RuntimeException(msg)

}
