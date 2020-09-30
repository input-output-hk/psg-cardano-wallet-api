package iog.psg.cardano

import java.time.ZonedDateTime

import io.circe.Decoder
import io.circe.parser._
import io.circe.syntax._
import iog.psg.cardano.CardanoApiCodec._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class CardanoApiCodecSpec extends AnyFlatSpec with Matchers {

  "Wallet" should "be decoded properly" in {
    val decoded = decodeJsonFile[Wallet]("wallet.json")

    compareWallets(decoded, wallet)
  }

  it should "decode wallet's list" in {
    val decodedWallets = decodeJsonFile[Seq[Wallet]]("wallets.json")

    decodedWallets.size shouldBe 1
    compareWallets(decodedWallets.head, wallet)
  }

  "network information" should "be decoded properly" in {
    val decoded = decodeJsonFile[NetworkInfo]("netinfo.json")

    compareNetworkInformation(
      decoded,
      NetworkInfo(
        syncProgress = SyncStatus(SyncState.ready, None),
        networkTip = networkTip.copy(height = None),
        nodeTip = nodeTip,
        nextEpoch = NextEpoch(ZonedDateTime.parse("2019-02-27T14:46:45.000Z"), 14)
      )
    )

  }

  "list addresses" should "be decoded properly" in {
    val decoded = decodeJsonFile[Seq[WalletAddressId]]("addresses.json")
    decoded.size shouldBe 1

    compareAddress(decoded.head, WalletAddressId(id = addressIdStr, Some(AddressFilter.used)))
  }

  "list transactions" should "be decoded properly" in {
    val decoded = decodeJsonFile[Seq[CreateTransactionResponse]]("transactions.json")
    decoded.size shouldBe 1

    compareTransaction(decoded.head, createdTransactionResponse)
  }

  it should "decode one transaction" in {
    val decoded = decodeJsonFile[CreateTransactionResponse]("transaction.json")

    compareTransaction(decoded, createdTransactionResponse)
  }

  "estimate fees" should "be decoded properly" in {
    val decoded = decodeJsonFile[EstimateFeeResponse]("estimate_fees.json")

    compareEstimateFeeResponse(decoded, estimateFeeResponse)
  }

  "fund payments" should "be decoded properly" in {
    val decoded = decodeJsonFile[FundPaymentsResponse]("coin_selections_random.json")

    compareFundPaymentsResponse(decoded, fundPaymentsResponse)
  }

  private def getJsonFromFile(file: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/jsons/$file"))
    val jsonStr = source.mkString
    source.close()
    jsonStr
  }


  private def decodeJsonFile[T](file: String)(implicit dec: Decoder[T]) = {
    val jsonStr = getJsonFromFile(file)
    decode[T](jsonStr).getOrElse(fail("Could not decode wallet"))
  }

  private def compareInAddress(decoded: InAddress, proper: InAddress) = {
    decoded.address shouldBe proper.address
    compareQuantityUnitOpts(decoded.amount, proper.amount)
    decoded.id shouldBe proper.id
    decoded.index shouldBe proper.index
  }

  private def compareOutAddress(decoded: OutAddress, proper: OutAddress) = {
    decoded.address shouldBe proper.address
    compareQuantityUnit(decoded.amount, proper.amount)
  }

  private def compareInputs(decoded: Seq[InAddress], proper: Seq[InAddress]) =
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareInAddress(decodedAddress, properAddress)
    }

  private def compareOutputs(decoded: Seq[OutAddress], proper: Seq[OutAddress]) =
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareOutAddress(decodedAddress, properAddress)
    }

  private def compareFundPaymentsResponse(decoded: FundPaymentsResponse, proper: FundPaymentsResponse) = {
    compareInputs(decoded.inputs, proper.inputs)
    compareOutputs(decoded.outputs, proper.outputs)
  }

  private def compareEstimateFeeResponse(decoded: EstimateFeeResponse, proper: EstimateFeeResponse) = {
    compareQuantityUnit(decoded.estimatedMax, proper.estimatedMax)
    compareQuantityUnit(decoded.estimatedMin, proper.estimatedMin)
  }

  private def compareStakeAddress(decoded: StakeAddress, proper: StakeAddress) = {
    compareQuantityUnit(decoded.amount, proper.amount)
    decoded.stakeAddress shouldBe proper.stakeAddress
  }

  private def compareStakeAddresses(decoded: Seq[StakeAddress], proper: Seq[StakeAddress]) = {
    decoded.zip(proper).map {
      case (decodedAddress, properAddress) => compareStakeAddress(decodedAddress, properAddress)
    }
  }

  private def compareTransaction(decoded: CreateTransactionResponse, proper: CreateTransactionResponse) = {
    decoded.id shouldBe proper.id
    compareQuantityUnit(decoded.amount, proper.amount)
    decoded.insertedAt shouldBe proper.insertedAt
    decoded.pendingSince shouldBe proper.pendingSince
    decoded.depth shouldBe proper.depth
    decoded.direction shouldBe proper.direction
    compareInputs(decoded.inputs, proper.inputs)
    compareOutputs(decoded.outputs, proper.outputs)
    compareStakeAddresses(decoded.withdrawals, proper.withdrawals)
    decoded.status shouldBe proper.status
    decoded.metadata shouldBe proper.metadata
  }

  private def compareAddress(decoded: WalletAddressId, proper: WalletAddressId) = {
    decoded.id shouldBe proper.id
    decoded.state shouldBe proper.state
  }

  private def compareNetworkInformation(decoded: NetworkInfo, proper: NetworkInfo) = {
    decoded.nextEpoch shouldBe proper.nextEpoch
    decoded.nodeTip shouldBe proper.nodeTip
    decoded.networkTip shouldBe proper.networkTip
    decoded.syncProgress.status.toString shouldBe proper.syncProgress.status.toString

    compareQuantityUnitOpts(decoded.syncProgress.progress, proper.syncProgress.progress)
  }

  private def compareQuantityUnitOpts(decoded: Option[QuantityUnit], proper: Option[QuantityUnit]) = {
    if (decoded.isEmpty && proper.isEmpty) assert(true)
    else for {
      decodedQU <- decoded
      properQU <- proper
    } yield compareQuantityUnit(decodedQU, properQU)
  }

  private def compareQuantityUnit(decoded: QuantityUnit, proper: QuantityUnit) = {
    decoded.unit.toString shouldBe proper.unit.toString
    decoded.quantity shouldBe proper.quantity
  }

  private def compareBalance(decoded: Balance, proper: Balance) = {
    decoded.available.quantity shouldBe proper.available.quantity
    decoded.available.unit.toString shouldBe proper.available.unit.toString

    decoded.reward.quantity shouldBe proper.reward.quantity
    decoded.reward.unit.toString shouldBe proper.reward.unit.toString

    decoded.total.quantity shouldBe proper.total.quantity
    decoded.total.unit.toString shouldBe proper.total.unit.toString
  }

  private def compareState(decoded: SyncStatus, proper: SyncStatus) = {
    decoded.status.toString shouldBe proper.status.toString
    decoded.progress shouldBe proper.progress
  }

  private def compareWallets(decoded: Wallet, proper: Wallet) = {
    decoded.id shouldBe proper.id
    decoded.addressPoolGap shouldBe proper.addressPoolGap
    compareBalance(decoded.balance, proper.balance)
    decoded.delegation shouldBe proper.delegation
    decoded.name shouldBe proper.name
    decoded.passphrase shouldBe proper.passphrase
    compareState(decoded.state, proper.state)
    decoded.tip shouldBe proper.tip
  }

  private final lazy val wallet = Wallet(
    id = "2512a00e9653fe49a44a5886202e24d77eeb998f",
    addressPoolGap = 20,
    balance = Balance(
      available = QuantityUnit(42000000, Units.lovelace),
      reward = QuantityUnit(42000000, Units.lovelace),
      total = QuantityUnit(42000000, Units.lovelace)
    ),
    delegation = Some(
      Delegation(
        active = DelegationActive(
          status = DelegationStatus.delegating,
          target = Some("1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1")
        ),
        next = List(
          DelegationNext(
            status = DelegationStatus.notDelegating,
            changesAt =
              Some(NextEpoch(epochStartTime = ZonedDateTime.parse("2020-01-22T10:06:39.037Z"), epochNumber = 14))
          )
        )
      )
    ),
    name = "Alan's Wallet",
    passphrase = Passphrase(lastUpdatedAt = ZonedDateTime.parse("2019-02-27T14:46:45.000Z")),
    state = SyncStatus(SyncState.ready, None),
    tip = networkTip
  )

  private final lazy val nextEpoch =
    NextEpoch(epochStartTime = ZonedDateTime.parse("2020-01-22T10:06:39.037Z"), epochNumber = 14)
  private final lazy val networkTip = NetworkTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = Some(QuantityUnit(1337, Units.block)),
    absoluteSlotNumber = Some(8086)
  )

  private final lazy val nodeTip = NodeTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = QuantityUnit(1337, Units.block),
    absoluteSlotNumber = Some(8086)
  )

  private final lazy val timedBlock = TimedBlock(
    time = ZonedDateTime.parse("2019-02-27T14:46:45.000Z"),
    block = Block(
      slotNumber = 1337,
      epochNumber = 14,
      height = QuantityUnit(1337, Units.block),
      absoluteSlotNumber = Some(8086)
    )
  )

  private final lazy val createdTransactionResponse = {
    val commonAmount = QuantityUnit(quantity = 42000000, unit = Units.lovelace)

    CreateTransactionResponse(
      id = "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1",
      amount = commonAmount,
      insertedAt = Some(timedBlock),
      pendingSince = Some(timedBlock),
      depth = Some(QuantityUnit(quantity = 1337, unit = Units.block)),
      direction = TxDirection.outgoing,
      inputs = Seq(inAddress),
      outputs = Seq(outAddress),
      withdrawals = Seq(
        StakeAddress(
          stakeAddress = "stake1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2x",
          amount = commonAmount
        )
      ),
      status = TxState.pending,
      metadata = Some(TxMetadataOut(json = parse("""
                                                   |{
                                                   |      "0": {
                                                   |        "string": "cardano"
                                                   |      },
                                                   |      "1": {
                                                   |        "int": 14
                                                   |      },
                                                   |      "2": {
                                                   |        "bytes": "2512a00e9653fe49a44a5886202e24d77eeb998f"
                                                   |      },
                                                   |      "3": {
                                                   |        "list": [
                                                   |          {
                                                   |            "int": 14
                                                   |          },
                                                   |          {
                                                   |            "int": 42
                                                   |          },
                                                   |          {
                                                   |            "string": "1337"
                                                   |          }
                                                   |        ]
                                                   |      },
                                                   |      "4": {
                                                   |        "map": [
                                                   |          {
                                                   |            "k": {
                                                   |              "string": "key"
                                                   |            },
                                                   |            "v": {
                                                   |              "string": "value"
                                                   |            }
                                                   |          },
                                                   |          {
                                                   |            "k": {
                                                   |              "int": 14
                                                   |            },
                                                   |            "v": {
                                                   |              "int": 42
                                                   |            }
                                                   |          }
                                                   |        ]
                                                   |      }
                                                   |    }
                                                   |""".stripMargin).getOrElse(fail("Invalid metadata json"))))
    )
  }

  private final lazy val estimateFeeResponse = {
    val commonAmount = QuantityUnit(quantity = 42000000, unit = Units.lovelace)

    EstimateFeeResponse(estimatedMin = commonAmount, estimatedMax = commonAmount)
  }

  private final lazy val fundPaymentsResponse =
    FundPaymentsResponse(inputs = IndexedSeq(inAddress), outputs = Seq(outAddress))

  private final lazy val inAddress = InAddress(
    address = Some(addressIdStr),
    amount = Some(QuantityUnit(quantity = 42000000, unit = Units.lovelace)),
    id = "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1",
    index = 0
  )

  private final lazy val outAddress =
    OutAddress(address = addressIdStr, amount = QuantityUnit(quantity = 42000000, unit = Units.lovelace))

  private final lazy val addressIdStr =
    "addr1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g"

}
