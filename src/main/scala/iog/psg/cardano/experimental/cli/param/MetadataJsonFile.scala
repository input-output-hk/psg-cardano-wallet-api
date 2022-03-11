package iog.psg.cardano.experimental.cli.param

import iog.psg.cardano.experimental.cli.util.CliCmdBuilder

import java.io.File

trait MetadataJsonFile {
  self: CliCmdBuilder =>

  lazy val jsonMetadataNoSchema: Out =
    build(_.withParam("--json-metadata-no-schema"))

  lazy val jsonMetadataDetailedSchema: Out =
    build(_.withParam("--json-metadata-detailed-schema"))

  def metadataJsonFile(metadataJson: File): Out =
    build(_.withParam("--metadata-json-file", metadataJson))

}
