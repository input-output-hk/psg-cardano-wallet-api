package iog.psg.cardano


import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, RequestEntity}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}
import iog.psg.cardano.CardanoApiCodec.{CreateRestore, ListAddresses, MnemonicSentence, Payment}

import scala.concurrent.{ExecutionContext, Future}


class CardanoApi(baseUriWithPort: String)(implicit ec: ExecutionContext) {

  private val wallets = s"${baseUriWithPort}wallets"
  private val network = s"${baseUriWithPort}network"
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames


  def listWallets: HttpRequest = HttpRequest(uri = wallets)

  def networkInfo: HttpRequest = HttpRequest(uri = s"${network}/information")

  def createRestoreWallet(
                           name: String,
                           passphrase: String,
                           mnemonicSentence: MnemonicSentence,
                           addressPoolGap: Option[Int] = None

  ): Future[HttpRequest] = {

    val createRestore =
      CreateRestore(
        name,
        passphrase,
        mnemonicSentence.mnemonicSentence,
        addressPoolGap
      )

    Marshal(createRestore).to[RequestEntity].map { marshalled =>
      HttpRequest(
        uri = s"${baseUriWithPort}wallets",
        method = POST,
        entity = marshalled
      )
    }

  }

  def listAddresses(listAddr: ListAddresses): Future[HttpRequest] = {
    Marshal(listAddr).to[RequestEntity] map { marshalled =>
      HttpRequest(
        uri = s"${wallets}/${listAddr.walletId}/addresses",
        method = GET,
        entity = marshalled
      )
    }
  }

  def fundPayments(walletId: String, payments: Seq[Payment]): Future[HttpRequest] = {
    Marshal(payments).to[RequestEntity] map { marshalled =>
      HttpRequest(
        uri = s"${wallets}/${walletId}/coin-selections/random",
        method = POST,
        entity = marshalled
      )
    }
  }
}
