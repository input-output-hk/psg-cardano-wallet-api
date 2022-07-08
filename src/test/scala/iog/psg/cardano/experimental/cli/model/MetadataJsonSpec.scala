package iog.psg.cardano.experimental.cli.model

import iog.psg.cardano.experimental.cli.model.MetadataJson.{Nft, NftFile, NftMetadataJson, asString}
import iog.psg.cardano.experimental.cli.util.RandomTempFolder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Files

class MetadataJsonSpec extends AnyFlatSpec with Matchers with ScalaFutures {

  val policyId = PolicyId("sfsdsdsdsdsdsd343434343434")

  def nft(tag: String) = Nft(s"label${tag}", s"name${tag}", s"image${tag}")

  val nfts = Array(1,2,3).map(i => nft(i.toString))
  val nftFile = NftFile("f", "audio/wav", Seq("uri"))
  nfts(0) = nfts(0).copy(description = Seq("short", "desc"), mediaType = Some("image/png"))
  nfts(1) = nfts(1).copy(description = Seq("onelinerdesc"),files = Seq(nftFile))

  implicit val rootFolder = RandomTempFolder(Files.createTempDirectory("testNFTMeta"))

  val meta = NftMetadataJson(policyId, nfts.toIndexedSeq)

  "the encoding" should "work as expected" in {
    val str = asString(meta)
    str shouldBe
      """{"721":
        |{"sfsdsdsdsdsdsd343434343434":
        | {"label1":
        |   {
        |     "name":"name1",
        |     "image":"image1",
        |     "mediaType":"image/png",
        |     "description":["short","desc"]
        |   },
        | "label2":
        |   {
        |     "name":"name2",
        |     "image":"image2",
        |     "description":"onelinerdesc",
        |     "files":[{
        |        "name":"f",
        |        "mediaType":"audio/wav",
        |        "src":"uri"
        |      }]
        |   },
        | "label3":
        |     {"name":"name3","image":"image3"}
        | }
        |}
        |}""".stripMargin.replaceAll("\\s","")
  }

}