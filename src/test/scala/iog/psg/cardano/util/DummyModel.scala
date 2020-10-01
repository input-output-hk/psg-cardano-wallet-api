package iog.psg.cardano.util

import java.time.ZonedDateTime

import iog.psg.cardano.CardanoApiCodec.{NetworkInfo, NetworkTip, NextEpoch, NodeTip, QuantityUnit, SyncState, SyncStatus, Units}

trait DummyModel {

  final lazy val dummyDateTime = ZonedDateTime.parse("2000-01-02T03:04:05.000Z")

  final lazy val networkTip = NetworkTip(
    epochNumber = 14,
    slotNumber = 1337,
    height = Some(QuantityUnit(1337, Units.block)),
    absoluteSlotNumber = Some(8086)
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
}
