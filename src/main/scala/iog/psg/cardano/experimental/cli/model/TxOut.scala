package iog.psg.cardano.experimental.cli.model

case class TxOut(address: String, output: Long, assets: List[NativeAsset] = Nil)
