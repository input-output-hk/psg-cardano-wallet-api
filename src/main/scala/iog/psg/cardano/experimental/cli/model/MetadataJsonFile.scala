package iog.psg.cardano.experimental.cli.model

import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.api.InFile
import iog.psg.cardano.experimental.cli.util.RandomTempFolder




sealed trait MetadataJson extends InFile

object MetadataJson {

  case class Nft(label: String,
                 name: String,
                 image: Seq[String],
                 mediaType: Option[String],
                 description: Seq[String],
                 files: Seq[NftFile])

  object Nft {
    def apply(label: String, name: String, image: String) =
      new Nft(label = label, name = name, image = Seq(image), mediaType = None, description = Seq.empty, files = Seq.empty)
  }

  case class NftFile(name: String, mediaType: String, src: Seq[String])

  case class NftMetadataJson (
                               policyId: PolicyId,
                               nfts: Seq[Nft])(implicit val rootFolder: RandomTempFolder) extends MetadataJson{

    override val content: String = MetadataJson.asString(this)
  }

  def asString(metadataJson: MetadataJson): String = metadataJson.asJson.noSpaces

  implicit val stringSeqEncoder: Encoder[Seq[String]] = (seq: Seq[String]) =>
    if (seq.size == 1) Encoder.encodeString(seq.head) else Encoder.encodeSeq[String].apply(seq)

  implicit val nftFileEncoder: Encoder[NftFile] = deriveEncoder

  implicit val encoder: Encoder[MetadataJson] = {

    case NftMetadataJson(policyId: PolicyId, nfts) =>
      Json.obj(("721",
      Json.obj(
        (policyId.value, Json.obj(nfts.map(nft =>
          (nft.label, Json.obj(
            ("name", nft.name.asJson),
            ("image", nft.image.asJson),
            ("mediaType", nft.mediaType.asJson),
            ("description", nft.description.asJson),
            ("files", nft.files.asJson)
          ).deepDropNullValues.dropEmptyValues)):_*
        )
        )
      )))
  }

}