package iog.psg.cardano.experimental.cli


trait NativeAssetsApi {

  type PolicyId = String
  type TokenName = String
  type CardanoAddress = String
  type Problem = String
  type Result[T] = Either[Problem, T]

  case class AssetId(policyId: PolicyId, name: TokenName)
  case class AssetBalance(id: AssetId, assetBalance: Long)

  def createNativeAsset(clientCred: ???, name: String, initialAmount: Long): Result[AssetBalance]

  def transfer(assetBalance: AssetBalance, toAddress: CardanoAddress): Result[AssetBalance]

  def burn(assetId: AssetId): Result[AssetBalance]

  def balance(assetId: AssetId): Result[AssetBalance]

}
