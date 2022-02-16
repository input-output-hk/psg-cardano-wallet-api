package iog.psg.cardano.experimental.cli.model

case class UTXO(txHash: String, txIx: Int, lovelace: Long, assets: List[NativeAsset] = Nil)
