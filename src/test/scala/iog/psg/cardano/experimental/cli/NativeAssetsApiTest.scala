package iog.psg.cardano.experimental.cli

import iog.psg.cardano.experimental.cli.command.CardanoCli
import iog.psg.cardano.experimental.cli.util.NetworkChooser
import iog.psg.cardano.experimental.nativeassets.{AssetBalance, NativeAssetsApi}

import java.nio.file.Paths

object NativeAssetsApiTest extends App {

  val cardano = CardanoCli(Paths.get("/Users/roman/Downloads/cardano-node-1.32.1-macos (2)/cardano-cli"))
    .withCardanoNodeSocketPath(Paths.get("/Users/roman/Library/Application Support/Daedalus Testnet/cardano-node.socket").toString)
    .withSudo(false)

  implicit val network: NetworkChooser = NetworkChooser.DefaultTestnet

  val workingDirectory = Paths.get("native-assets-api-test")
  val paymentVerKey = workingDirectory.resolve("payment.vkey").toFile
  val paymentSignKey = workingDirectory.resolve("payment.skey").toFile

  val paymentAddress = "addr_test1vrxl2yr3nz545j8yej0c0jcqxhkq3t27hxaz9ry8lfdezwqf6zchv"
  // cardano.genPaymentKeysAndAddress(paymentVerKey, paymentSignKey)

  val nativeAssetsApi = NativeAssetsApi.apply(
    cardano = cardano,
    network = network,
    workingDir = workingDirectory,
    paymentAddress = paymentAddress,
    paymentVerKey = paymentVerKey,
    paymentSignKey = paymentSignKey,
  )

  val createdAsset = nativeAssetsApi
    .createNativeAsset(name = "native-asset-test-v7", amount = 30)
    .getOrElse(throw new RuntimeException("failed to mint native asset"))

  println(createdAsset)

  import scala.concurrent.duration._
  Thread.sleep(2.minutes.toMillis)

  val burnedAsset = nativeAssetsApi.transfer(
    AssetBalance(createdAsset.id, 13),
    "addr_test1qqywjhcz3nqa3tfc6sw0jymqgxayr4j8c8lezqtyzzvpd8s67xch0swzl6qyheq3zmvysnw775lva5cjcganffnvdn8q7hpe8e"
  ).getOrElse(throw new RuntimeException("failed to burn native asset"))

  println(burnedAsset)
}
