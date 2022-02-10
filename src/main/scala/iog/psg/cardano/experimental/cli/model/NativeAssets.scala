package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList

case class NativeAssets(policyId: String, assets: NonEmptyList[NativeAsset])
