package iog.psg.cardano.model

import akka.util.ByteString
import io.circe.CursorOp.DownField
import io.circe._
import iog.psg.cardano.codecs.CardanoApiCodec.{MetadataKey, MetadataValue, MetadataValueArray, MetadataValueByteString, MetadataValueLong, MetadataValueMap, MetadataValueStr}

final case class TxMetadataOut(json: Json) {
  def toMapMetadataStr: Decoder.Result[Map[Long, MetadataValue]] = {
    type KeyVal = Map[Long, MetadataValue]

    // using the expansion may be necessary for Circe to detect it correctly
    implicit val decodeMap: Decoder[Map[Long, MetadataValue]] = (c: HCursor) => {

      val valueTypeString = "string"
      val valueTypeLong = "int" //named int but will work as long
      val valueTypeBytes = "bytes"
      val valueTypeList = "list"
      val valueTypeMap = "map"

      def extractStringField(cursor: ACursor): Either[DecodingFailure, MetadataValueStr] =
        cursor.downField(valueTypeString).as[String].fold(
          err => Left(err),
          (value: String) => Right(MetadataValueStr(value))
        )

      def extractLongField(cursor: ACursor): Either[DecodingFailure, MetadataValueLong] =
        cursor.downField(valueTypeLong).as[Long].fold(
          err => Left(err),
          (value: Long) => Right(MetadataValueLong(value))
        )

      def extractBytesField(cursor: ACursor): Either[DecodingFailure, MetadataValueByteString] =
        cursor.downField(valueTypeBytes).as[String].fold(
          err => Left(err),
          (value: String) => Right(MetadataValueByteString(ByteString(value)))
        )

      def extractTypedFieldValue(json: Json): Either[DecodingFailure, MetadataValue] = {
        val cursor = json.hcursor
        cursor.keys.flatMap(_.headOption) match {
          case Some(valueType) if valueType == valueTypeString =>
            extractStringField(cursor)

          case Some(valueType) if valueType == valueTypeLong =>
            extractLongField(cursor)

          case Some(valueType) if valueType == valueTypeBytes =>
            extractBytesField(cursor)
        }
      }

      def extractValueForKeyInto(res: Decoder.Result[KeyVal], key: String): Decoder.Result[KeyVal] = {
        res.flatMap((map: KeyVal) => {
          val keyDownField: ACursor = c.downField(key)
          keyDownField.keys.flatMap(_.headOption) match {
            case Some(valueType) if valueType == valueTypeString =>
              extractStringField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == valueTypeLong =>
              extractLongField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == valueTypeBytes =>
              extractBytesField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == valueTypeList =>
              val downFieldList = keyDownField.downField(valueTypeList)
              val keyValuesObjects: List[Json] = downFieldList.values.map(_.toList).getOrElse(Nil)

              val listResults: Seq[Either[DecodingFailure, MetadataValue]] = keyValuesObjects.map(extractTypedFieldValue)

              val errors = listResults.filter(_.isLeft)
              if (errors.nonEmpty) Left(errors.head.swap.toOption.get)
              else {
                val values = listResults.flatMap(_.toOption)
                Right(map.+(key.toLong -> MetadataValueArray(values)))
              }

            case Some(valueType) if valueType == valueTypeMap =>
              val downFieldMap = keyDownField.downField(valueTypeMap)
              val keyValuesObjects: List[Json] = downFieldMap.values.map(_.toList).getOrElse(Nil)

              def getMapField[T <: MetadataValue](keyName: String, json: Json) = for {
                keyJson <- json.\\(keyName).headOption.toRight(DecodingFailure(s"Missing '$keyName' value", List(DownField(key))))
                value <- extractTypedFieldValue(keyJson)
              } yield value

              val results: Seq[Either[DecodingFailure, (MetadataKey, MetadataValue)]] = keyValuesObjects.map { json =>
                (getMapField[MetadataKey]("k", json), getMapField[MetadataValue]("v", json)) match {
                  case (Right(keyField), Right(valueField)) => Right(keyField.asInstanceOf[MetadataKey] -> valueField)
                  case (Left(error), _) => Left(error)
                  case (_, Left(error)) => Left(error)
                }
              }
              val errors = results.filter(_.isLeft)
              if (errors.nonEmpty) Left(errors.head.swap.toOption.get)
              else {
                val values: Map[MetadataKey, MetadataValue] = results.flatMap(_.toOption).toMap
                Right(map.+(key.toLong -> MetadataValueMap(values)))
              }

            case None => Left(DecodingFailure("Missing value under key", List(DownField(key))))
          }
        })
      }

      def emptyMapResult: Decoder.Result[KeyVal] = Right(Map[Long, MetadataValue]().empty)

      def withKeys(keys: Iterable[String]): Decoder.Result[KeyVal] = keys.foldLeft(emptyMapResult)(extractValueForKeyInto)

      c.keys.fold[Decoder.Result[KeyVal]](ifEmpty = emptyMapResult)(withKeys)
    }

    json.as[Map[Long, MetadataValue]](decodeMap)
  }
}

