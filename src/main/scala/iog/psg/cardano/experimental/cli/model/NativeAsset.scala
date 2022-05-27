package iog.psg.cardano.experimental.cli.model

case class AssetId(policyId: String, name: Base16String)

case class NativeAsset(
  assetId: AssetId,
  tokenAmount: Long
)
