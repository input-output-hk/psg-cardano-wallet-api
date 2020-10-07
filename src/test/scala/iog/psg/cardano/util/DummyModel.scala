package iog.psg.cardano.util

import java.time.ZonedDateTime

import io.circe.parser.parse
import iog.psg.cardano.CardanoApiCodec._
import iog.psg.cardano.TxMetadataOut
import org.scalatest.Assertions

trait DummyModel { self: Assertions =>

  final lazy val dummyDateTime = ZonedDateTime.parse("2000-01-02T03:04:05.000Z")

  final val addressIdStr =
    "addr1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g"

  final val inAddress = InAddress(
    address = Some(addressIdStr),
    amount = Some(QuantityUnit(quantity = 42000000, unit = Units.lovelace)),
    id = "1423856bc91c49e928f6f30f4e8d665d53eb4ab6028bd0ac971809d514c92db1",
    index = 0
  )

  final lazy val outAddress =
    OutAddress(address = addressIdStr, amount = QuantityUnit(quantity = 42000000, unit = Units.lovelace))

  final lazy val timedBlock = TimedBlock(
    time = dummyDateTime,
    block = Block(
      slotNumber = 1337,
      epochNumber = 14,
      height = QuantityUnit(1337, Units.block),
      absoluteSlotNumber = Some(8086)
    )
  )

  final lazy val txMetadataOut = TxMetadataOut(json = parse("""
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
                                                         |""".stripMargin).getOrElse(fail("Invalid metadata json")))

  final lazy val createdTransactionResponse = {
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
      metadata = Some(txMetadataOut)
    )
  }


  final val addresses = Seq(
    WalletAddressId(
      id = "addr1sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g",
      state = Some(AddressFilter.unUsed)
    ),
    WalletAddressId(
      id = "addr2sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g",
      state = Some(AddressFilter.used)
    ),
    WalletAddressId(
      id = "addr3sjck9mdmfyhzvjhydcjllgj9vjvl522w0573ncustrrr2rg7h9azg4cyqd36yyd48t5ut72hgld0fg2xfvz82xgwh7wal6g2xt8n996s3xvu5g",
      state = Some(AddressFilter.unUsed)
    )
  )

  final lazy val unUsedAddresses = addresses.filter(_.state.contains(AddressFilter.unUsed))
  final lazy val usedAddresses = addresses.filter(_.state.contains(AddressFilter.used))

  final lazy val networkTip = NetworkTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = Some(QuantityUnit(1337, Units.block)),
    absoluteSlotNumber = Some(8086)
  )

  final lazy val wallet = Wallet(
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

  final lazy val nodeTip = NodeTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = QuantityUnit(1337, Units.block),
    absoluteSlotNumber = Some(8086)
  )

  final lazy val networkInfo = NetworkInfo(
    syncProgress = SyncStatus(SyncState.ready, None),
    networkTip = networkTip.copy(height = None),
    nodeTip = nodeTip,
    nextEpoch = NextEpoch(dummyDateTime, 14)
  )

  final lazy val mnemonicSentence = GenericMnemonicSentence("a b c d e a b c d e a b c d e")

  final lazy val payments = Payments(Seq(Payment(unUsedAddresses.head.id, QuantityUnit(100000, Units.lovelace))))

  final lazy val estimateFeeResponse = {
    val estimatedMin = QuantityUnit(quantity = 42000000, unit = Units.lovelace)
    EstimateFeeResponse(
      estimatedMin = estimatedMin,
      estimatedMax = estimatedMin.copy(quantity = estimatedMin.quantity * 3)
    )
  }

  final lazy val fundPaymentsResponse =
    FundPaymentsResponse(inputs = IndexedSeq(inAddress), outputs = Seq(outAddress))

}
