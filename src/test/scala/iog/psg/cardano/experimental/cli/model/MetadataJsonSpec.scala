package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.model.MetadataJson.{Nft, NftMetadataJson, asString}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Files

class MetadataJsonSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  val policyId = PolicyId("sfsdsdsdsdsdsd343434343434")

  def nft(tag: String) = Nft(s"label${tag}", s"name${tag}", s"image${tag}")

  val nfts = Seq(1,2,3).map(i => nft(i.toString))

  implicit val rootFolder = RandomTempFolder(Files.createTempDirectory("testNFTMeta"))

  val meta = NftMetadataJson(policyId, nfts)

  "the encoding" should "work as expected" in {
    val str = asString(meta)
    str shouldBe
      """{"721":{"sfsdsdsdsdsdsd343434343434":{"label1":{"name":"name1","image":"image1"},"label2":{"name":"name2","image":"image2"},"label3":{"name":"name3","image":"image3"}}}}"""
  }

}