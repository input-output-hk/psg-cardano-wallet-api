package iog.psg.cardano.experimental.cli.model

import cats.data.NonEmptyList
import io.circe._
import io.circe.syntax._
import iog.psg.cardano.experimental.cli.api.InFile
import iog.psg.cardano.experimental.cli.util.RandomTempFolder




sealed trait MetadataJson extends InFile

object MetadataJson {

  case class Nft(label: String, name: String, image: String)

  case class NftMetadataJson (
                               policyId: PolicyId,
                               nfts: Seq[Nft])(implicit val rootFolder: RandomTempFolder) extends MetadataJson{

    override val content: String = MetadataJson.asString(this)
  }

  def asString(metadataJson: MetadataJson): String = metadataJson.asJson.noSpaces

  implicit val encoder: Encoder[MetadataJson] = {

    case NftMetadataJson(policyId: PolicyId, nfts) =>
      Json.obj(("721",
      Json.obj(
        (policyId.value, Json.obj(nfts.map(nft =>
          (nft.label, Json.obj(
            ("name", nft.name.asJson),
            ("image", nft.image.asJson)
          ))):_*
        )
        )
      )))
  }
}