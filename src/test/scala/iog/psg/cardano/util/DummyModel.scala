package iog.psg.cardano.util

import java.time.ZonedDateTime

import iog.psg.cardano.CardanoApiCodec._

trait DummyModel {

  final val wallet = Wallet(
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

  final val networkTip = NetworkTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = Some(QuantityUnit(1337, Units.block)),
    absoluteSlotNumber = Some(8086)
  )
}
