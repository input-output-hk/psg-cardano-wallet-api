package iog.psg.cardano

import java.io.File
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.CardanoApiOps.{CardanoApiRequestFOps, CardanoApiRequestOps}
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage, Order, defaultMaxWaitTime}
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, GenericMnemonicSentence, Payment, Payments, QuantityUnit, Units}
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
    val baseUrl = "-baseUrl"
    val listWallets = "-wallets"
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
    val addressPoolGap = "-addressPoolGap"
    val listWalletAddresses = "-listAddresses"
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
    val txId = "-txId"
    val amount = "-amount"
    val address = "-address"
  }

  val defaultBaseUrl = "http://127.0.0.1:8090/v2/"
  val defaultTraceFile = "cardano-api.log"

  def main(args: Array[String]): Unit = {

    val arguments = new ArgumentParser(args)
    val conTracer = if (arguments.contains(CmdLine.noConsole)) NoOpTrace else ConsoleTrace

    implicit val trace = conTracer.withTrace(
      if (arguments.contains(CmdLine.traceToFile)) {
        val fileName = arguments(CmdLine.traceToFile).getOrElse(defaultTraceFile)
        new FileTrace(new File(fileName))
      } else NoOpTrace
    )

    run(arguments)

  }


  private[cardano] def run(arguments: ArgumentParser)(implicit trace: Trace): Unit = {


    if (arguments.noArgs || arguments.contains(CmdLine.help)) {
      showHelp()
    } else {

      def hasArgument(arg: String): Boolean = {
        val result = arguments.contains(arg)
        if (result) trace(arg)
        result
      }

      implicit val system: ActorSystem = ActorSystem("SingleRequest")
      import system.dispatcher //the

      Try {

        val url = arguments(CmdLine.baseUrl).getOrElse(defaultBaseUrl)

        trace(s"baseurl:$url")

        val api = new CardanoApi(url)


        if (hasArgument(CmdLine.netInfo)) {
          val result = unwrap(api.networkInfo.executeBlocking)
          trace(result)
        } else if (hasArgument(CmdLine.listWallets)) {
          val result = unwrap(api.listWallets.executeBlocking)
          result.foreach(trace.apply)

        } else if (hasArgument(CmdLine.estimateFee)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))
          val result = unwrap(api.estimateFee(walletId, payments).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.getWallet)) {
          val walletId = arguments.get(CmdLine.walletId)
          val result = unwrap(api.getWallet(walletId).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.updatePassphrase)) {
          val walletId = arguments.get(CmdLine.walletId)
          val oldPassphrase = arguments.get(CmdLine.oldPassphrase)
          val newPassphrase = arguments.get(CmdLine.passphrase)

          val result: Unit = unwrap(api.updatePassphrase(walletId, oldPassphrase, newPassphrase).executeBlocking)
          trace("Unit result from delete wallet")

        } else if (hasArgument(CmdLine.deleteWallet)) {
          val walletId = arguments.get(CmdLine.walletId)
          val result: Unit = unwrap(api.deleteWallet(walletId).executeBlocking)
          trace("Unit result from delete wallet")

        } else if (hasArgument(CmdLine.listWalletAddresses)) {
          val walletId = arguments.get(CmdLine.walletId)
          val addressesState = Some(AddressFilter.withName(arguments.get(CmdLine.state)))
          val result = unwrap(api.listAddresses(walletId, addressesState).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.getTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val txId = arguments.get(CmdLine.txId)
          val result = unwrap(api.getTransaction(walletId, txId).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.createTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val pass = arguments.get(CmdLine.passphrase)
          val metadata = toMetaMap(arguments(CmdLine.metadata))
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))
          val result = unwrap(api.createTransaction(
            walletId,
            pass,
            payments,
            metadata,
            None
          ).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.fundTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val amount = arguments.get(CmdLine.amount).toLong
          val addr = arguments.get(CmdLine.address)
          val singlePayment = Payment(addr, QuantityUnit(amount, Units.lovelace))
          val payments = Payments(Seq(singlePayment))
          val result = unwrap(api.fundPayments(
            walletId,
            payments
          ).executeBlocking)
          trace(result)


        } else if (hasArgument(CmdLine.listWalletTransactions)) {
          val walletId = arguments.get(CmdLine.walletId)
          val startDate = arguments(CmdLine.start).map(strToZonedDateTime)
          val endDate = arguments(CmdLine.end).map(strToZonedDateTime)
          val orderOf = arguments(CmdLine.order).flatMap(s => Try(Order.withName(s)).toOption).getOrElse(Order.descendingOrder)
          val minWithdrawalTx = arguments(CmdLine.minWithdrawal).map(_.toInt)

          val result = unwrap(api.listTransactions(
            walletId,
            startDate,
            endDate,
            orderOf,
            minWithdrawal = minWithdrawalTx
          ).executeBlocking)

          if (result.isEmpty) {
            trace("No txs returned")
          } else {
            result.foreach(trace.apply)
          }

        } else if (hasArgument(CmdLine.listWallets)) {
          val result = unwrap(api.listWallets.executeBlocking)
          result.foreach(trace.apply)

        } else if (hasArgument(CmdLine.createWallet) || hasArgument(CmdLine.restoreWallet)) {
          val name = arguments.get(CmdLine.name)
          val passphrase = arguments.get(CmdLine.passphrase)
          val mnemonic = arguments.get(CmdLine.mnemonic)
          val addressPoolGap = arguments(CmdLine.addressPoolGap).map(_.toInt)

          val result = unwrap(api.createRestoreWallet(
            name,
            passphrase,
            GenericMnemonicSentence(mnemonic),
            addressPoolGap
          ).executeBlocking)

          trace(result)

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

  private def showHelp()(implicit trace: Trace): Unit = {
    val exampleWalletId = "1234567890123456789012345678901234567890"
    val exampleTxd = "ABCDEF1234567890"
    val exampleAddress = "addr12345678901234567890123456789012345678901234567890123456789012345678901234567890"
    val exampleMetadata = "0:0123456789012345678901234567890123456789012345678901234567890123:2:TESTINGCARDANOAPI"
    val exampleMnemonic = "ability make always any pulse swallow marriage media dismiss degree edit spawn distance state dad"

    trace("This super simple tool allows developers to access a cardano wallet backend from the command line\n")
    trace("Usage:")
    trace("export CMDLINE='java -jar psg-cardano-wallet-api-assembly-<VER>.jar'")
    trace("$CMDLINE [command] [arguments]\n")

    def beautifyTrace(commandRunDesc: String, description: String, examples: List[String], apiDocOperation: String = ""): Unit = {
      val docsUrl = if (apiDocOperation.nonEmpty) s" [ https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/$apiDocOperation ]\n" else ""
      val examplesStr = s" Examples:\n ${examples.map("$CMDLINE "+_).mkString("\n ")}"
      trace(s"$commandRunDesc\n $description\n$docsUrl\n$examplesStr\n")
    }

    trace("Optional commands:")
    beautifyTrace(
      commandRunDesc = s"${CmdLine.traceToFile} [filename] [command]",
      description = s"write logs into a defined file ( default file name: ${CardanoApiMain.defaultTraceFile} )",
      examples = List(
        s"${CmdLine.traceToFile} wallets.log ${CmdLine.listWallets}"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.baseUrl} [url] [command]",
      description = s"define different api url ( default : ${CardanoApiMain.defaultBaseUrl} )",
      examples = List(
        s"${CmdLine.baseUrl} http://cardano-wallet-testnet.mydomain:8090/v2/ ${CmdLine.listWallets}"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.noConsole} [command]",
      description = "run a command without any logging",
      examples = List(
        s"${CmdLine.noConsole} ${CmdLine.deleteWallet} ${CmdLine.walletId} $exampleWalletId"
      )
    )

    trace("Commands:")
    beautifyTrace(
      commandRunDesc = s"${CmdLine.netInfo}",
      description = "Show network information",
      apiDocOperation = "getNetworkInformation",
      examples = List(
        s"${CmdLine.netInfo}"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.listWallets}",
      description = "Return a list of known wallets, ordered from oldest to newest",
      apiDocOperation = "listWallets",
      examples = List(
        s"${CmdLine.listWallets}"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.estimateFee} ${CmdLine.walletId} [walletId] ${CmdLine.amount} [amount] ${CmdLine.address} [address]",
      description = "Estimate fee for the transaction",
      apiDocOperation = "postTransactionFee",
      examples = List(
        s"${CmdLine.estimateFee} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.getWallet} ${CmdLine.walletId} [walletId]",
      description = "Get wallet by id",
      apiDocOperation = "getWallet",
      examples = List(
        s"${CmdLine.getWallet} ${CmdLine.walletId} $exampleWalletId"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.updatePassphrase} ${CmdLine.walletId} [walletId] ${CmdLine.oldPassphrase} [oldPassphrase] ${CmdLine.passphrase} [newPassphrase]",
      description = "Update passphrase",
      apiDocOperation = "putWalletPassphrase",
      examples = List(
        s"${CmdLine.updatePassphrase} ${CmdLine.walletId} $exampleWalletId ${CmdLine.oldPassphrase} OldPassword12345! ${CmdLine.passphrase} NewPassword12345!]"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.deleteWallet} ${CmdLine.walletId} [walletId]",
      description = "Delete wallet by id",
      apiDocOperation = "deleteWallet",
      examples = List(
        s"${CmdLine.deleteWallet} ${CmdLine.walletId} $exampleWalletId"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} [walletId] ${CmdLine.state} [state]",
      description = "Return a list of known addresses, ordered from newest to oldest, state: used, unused",
      apiDocOperation = "listAddresses",
      examples = List(
        s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} $exampleWalletId ${CmdLine.state} ${AddressFilter.used}",
        s"${CmdLine.listWalletAddresses} ${CmdLine.walletId} $exampleWalletId ${CmdLine.state} ${AddressFilter.unUsed}"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.getTx} ${CmdLine.walletId} [walletId] ${CmdLine.txId} [txId]",
      description = "Get transaction by id",
      apiDocOperation = "getTransaction",
      examples = List(
        s"${CmdLine.getTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.txId} $exampleTxd"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.createTx} ${CmdLine.walletId} [walletId] ${CmdLine.amount} [amount] ${CmdLine.address} [address] ${CmdLine.passphrase} [passphrase] ${CmdLine.metadata} [metadata](optional)",
      description = "Create and send transaction from the wallet",
      apiDocOperation = "postTransaction",
      examples = List(
        s"${CmdLine.createTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress ${CmdLine.passphrase} Password12345!",
        s"${CmdLine.createTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress ${CmdLine.passphrase} Password12345! ${CmdLine.metadata} $exampleMetadata",
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.fundTx} ${CmdLine.walletId} [walletId] ${CmdLine.amount} [amount] ${CmdLine.address} [address]",
      description = "Select coins to cover the given set of payments",
      apiDocOperation = "selectCoins",
      examples = List(
        s"${CmdLine.fundTx} ${CmdLine.walletId} $exampleWalletId ${CmdLine.amount} 20000 ${CmdLine.address} $exampleAddress"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.listWalletTransactions} ${CmdLine.walletId} [walletId] ${CmdLine.start} [start_date](optional) ${CmdLine.end} [end_date](optional) ${CmdLine.order} [order](optional) ${CmdLine.minWithdrawal} [minWithdrawal](optional)",
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
    beautifyTrace(
      commandRunDesc = s"${CmdLine.createWallet} ${CmdLine.name} [walletName] ${CmdLine.passphrase} [passphrase] ${CmdLine.mnemonic} [mnemonic] ${CmdLine.addressPoolGap} [address_pool_gap](optional)",
      description = "Create new wallet",
      apiDocOperation = "postWallet",
      examples = List(
        s"${CmdLine.createWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic'",
        s"${CmdLine.createWallet} ${CmdLine.name} new_wallet_2 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.addressPoolGap} 10"
      )
    )
    beautifyTrace(
      commandRunDesc = s"${CmdLine.restoreWallet} ${CmdLine.name} [walletName] ${CmdLine.passphrase} [passphrase] ${CmdLine.mnemonic} [mnemonic] ${CmdLine.addressPoolGap} [address_pool_gap](optional)",
      description = "Restore wallet",
      apiDocOperation = "postWallet",
      examples = List(
        s"${CmdLine.restoreWallet} ${CmdLine.name} new_wallet_1 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic''",
        s"${CmdLine.restoreWallet} ${CmdLine.name} new_wallet_2 ${CmdLine.passphrase} Password12345! ${CmdLine.mnemonic} '$exampleMnemonic' ${CmdLine.addressPoolGap} 10"
      )
    )
  }

  def unwrap[T: ClassTag](apiResult: CardanoApiResponse[T])(implicit t: Trace): T = unwrapOpt(Try(apiResult)).get

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
