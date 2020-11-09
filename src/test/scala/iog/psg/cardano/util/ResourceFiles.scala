package iog.psg.cardano.util

import io.circe.Decoder
import io.circe.parser.decode
import iog.psg.cardano.CardanoApiCodec._
import org.scalatest.Assertions

import scala.io.Source

trait ResourceFiles { self: Assertions =>

  final lazy val jsonFileWallet = decodeJsonFile[Wallet]("wallet.json")
  final lazy val jsonFileCreatedTransactionResponse = decodeJsonFile[CreateTransactionResponse]("transaction.json")
  final lazy val jsonFileCreatedTransactionsResponse = decodeJsonFile[Seq[CreateTransactionResponse]]("transactions.json")
  final lazy val jsonFileProxyTransactionResponse = decodeJsonFile[PostExternalTransactionResponse]("proxy_trans_resp.json")
  final lazy val jsonFileMigrationResponse = decodeJsonFile[MigrationResponse]("migration.json")
  final lazy val jsonFileMigrationsResponse = decodeJsonFile[Seq[MigrationResponse]]("migrations.json")
  final lazy val jsonFileMigrationCostsResponse = decodeJsonFile[MigrationCostResponse]("migration_costs.json")
  final lazy val jsonFileStakePoolsResponse = decodeJsonFile[Seq[StakePool]]("stake_pools.json")

  final lazy val stakePoolId = "pool1wqaz0q0zhtxlgn0ewssevn2mrtm30fgh2g7hr7z9rj5856457mm"

  final lazy val txRawContent = getFileContent("tx.raw")

  final def getJsonFromFile(file: String): String =
    getFileContent(s"jsons/$file")

  final def decodeJsonFile[T](file: String)(implicit dec: Decoder[T]): T = {
    val jsonStr = getJsonFromFile(file)
    decode[T](jsonStr) match {
      case Left(error) => fail(s"Could not decode $file error: $error")
      case Right(value) => value
    }
  }

  final def getFileContent(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/$file"))
    val sourceStr = source.mkString
    source.close()
    sourceStr
  }
}
