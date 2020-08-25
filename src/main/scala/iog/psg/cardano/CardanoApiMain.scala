package iog.psg.cardano

import java.io.File

import akka.actor.ActorSystem
import iog.psg.cardano.CardanoApi.{CardanoApiResponse, ErrorMessage, IOExecutionContext, Order, defaultMaxWaitTime}
import iog.psg.cardano.CardanoApiCodec.{AddressFilter, GenericMnemonicSentence, Payment, Payments, QuantityUnit, Units}
import iog.psg.cardano.util.{ArgumentParser, ConsoleTrace, FileTrace, NoOpTrace, Trace}

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
    val getWallet = "-wallet"
    val createWallet = "-createWallet"
    val restoreWallet = "-restoreWallet"
    val name = "-name"
    val passphrase = "-passphrase"
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
      implicit val ioEc: IOExecutionContext = IOExecutionContext(system.dispatcher)

      Try {

        val url = arguments(CmdLine.baseUrl).getOrElse(defaultBaseUrl)

        trace(s"baseurl:$url")

        val api = new CardanoApi(url)
        import api.Ops._

        if (hasArgument(CmdLine.netInfo)) {
          val result = unwrap(api.networkInfo.executeBlocking)
          trace(result.toString)
        } else if (hasArgument(CmdLine.listWallets)) {
          val result = unwrap(api.listWallets.executeBlocking)
          result.foreach(trace.apply)
        } else if (hasArgument(CmdLine.walletId)) {
          val result = unwrap(api.listWallets.executeBlocking)
          result.foreach(trace.apply)


        } else if (hasArgument(CmdLine.getWallet)) {
          val walletId = arguments.get(CmdLine.walletId)
          val result = unwrap(api.getWallet(walletId).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.getTx)) {
          val walletId = arguments.get(CmdLine.walletId)
          val txId = arguments.get(CmdLine.getTx)
          val result = unwrap(api.getTransaction(walletId, txId).executeBlocking)
          trace(result)

        } else if (hasArgument(CmdLine.createTx)) {
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
          //val startDate = arguments(start)
          //val endDate = arguments(end)
          val orderOf = arguments(CmdLine.order).flatMap(s => Try(Order.withName(s)).toOption).getOrElse(Order.descendingOrder)
          val minWithdrawalTx = arguments(CmdLine.minWithdrawal).map(_.toInt).getOrElse(1)

          val result = unwrap(api.listTransactions(
            walletId = walletId,
            order = orderOf,
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


  def showHelp(): Unit = {
    println("Enter commands, and so on...")
  }


  def unwrap[T: ClassTag](apiResult: CardanoApiResponse[T])(implicit t: Trace): T = unwrapOpt(Try(apiResult)).get

  def unwrapOpt[T:ClassTag](apiResult: Try[CardanoApiResponse[T]])(implicit trace: Trace): Option[T] = apiResult match {
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
