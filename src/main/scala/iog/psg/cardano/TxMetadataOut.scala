package iog.psg.cardano

import akka.util.ByteString
import io.circe.CursorOp.DownField
import io.circe._
import iog.psg.cardano.CardanoApiCodec._

final case class TxMetadataOut(json: Json) {

  type DecodingEither[T] = Either[DecodingFailure, T]

  def toMapMetadataStr: Decoder.Result[Map[Long, MetadataValue]] = {
    type KeyVal = Map[Long, MetadataValue]

    // using the expansion may be necessary for Circe to detect it correctly
    implicit val decodeMap: Decoder[Map[Long, MetadataValue]] = (c: HCursor) => {

      val ValueTypeString = "string"
      val ValueTypeLong = "int" //named int but will work as long
      val ValueTypeBytes = "bytes"
      val ValueTypeList = "list"
      val ValueTypeMap = "map"

      def extractStringField(cursor: ACursor): DecodingEither[MetadataValueStr] =
        cursor.downField(ValueTypeString).as[String].fold(
          err => Left(err),
          (value: String) => Right(MetadataValueStr(value))
        )

      def extractLongField(cursor: ACursor): DecodingEither[MetadataValueLong] =
        cursor.downField(ValueTypeLong).as[Long].fold(
          err => Left(err),
          (value: Long) => Right(MetadataValueLong(value))
        )

      def extractBytesField(cursor: ACursor): DecodingEither[MetadataValueByteString] =
        cursor.downField(ValueTypeBytes).as[String].fold(
          err => Left(err),
          (value: String) => Right(MetadataValueByteString(ByteString(value)))
        )

      def extractListField(cursor: ACursor): DecodingEither[MetadataValueArray] = {
        val keyValuesObjects: List[Json] = cursor.downField(ValueTypeList).values.map(_.toList).getOrElse(Nil)
        val listResults: Seq[DecodingEither[MetadataValue]] = keyValuesObjects.map(extractTypedFieldValue)

        listMapErrorOrResult(listResults, () => {
          val values = listResults.flatMap(_.toOption)
          MetadataValueArray(values)
        })
      }

      def extractMapField(cursor: ACursor, key: String): DecodingEither[MetadataValueMap] = {
        val downFieldMap = cursor.downField(ValueTypeMap)
        val keyValuesObjects: List[Json] = downFieldMap.values.map(_.toList).getOrElse(Nil)

        def getMapField[T <: MetadataValue](keyName: String, json: Json): DecodingEither[MetadataValue] = for {
          keyJson <- json.\\(keyName).headOption.toRight(DecodingFailure(s"Missing '$keyName' value", List(DownField(key))))
          value <- extractTypedFieldValue(keyJson)
        } yield value

        val listResults: Seq[DecodingEither[(MetadataKey, MetadataValue)]] = keyValuesObjects.map { json =>
          (getMapField[MetadataKey]("k", json), getMapField[MetadataValue]("v", json)) match {
            case (Right(keyField), Right(valueField)) => Right(keyField.asInstanceOf[MetadataKey] -> valueField)
            case (Left(error), _) => Left(error)
            case (_, Left(error)) => Left(error)
          }
        }

        listMapErrorOrResult(listResults, () => {
          val values: Map[MetadataKey, MetadataValue] = listResults.flatMap(_.toOption).toMap
          MetadataValueMap(values)
        })
      }

      def extractTypedFieldValue(json: Json): DecodingEither[MetadataValue] = {
        val cursor = json.hcursor
        cursor.keys.flatMap(_.headOption) match {
          case Some(ValueTypeString) =>
            extractStringField(cursor)

          case Some(ValueTypeLong) =>
            extractLongField(cursor)

          case Some(ValueTypeBytes)  =>
            extractBytesField(cursor)
        }
      }

      /**
       * If any of parsed values in list or map contains an error, it will return it
       */
      def listMapErrorOrResult[A, B](results: Seq[DecodingEither[A]], onRight: () => B): DecodingEither[B] =
        results.find(_.isLeft) match {
          case Some(Left(error)) => Left(error)
          case None => Right(onRight())
        }

      def extractValueForKeyInto(res: Decoder.Result[KeyVal], key: String): Decoder.Result[KeyVal] = {
        res.flatMap((map: KeyVal) => {
          val keyDownField: ACursor = c.downField(key)
          keyDownField.keys.flatMap(_.headOption) match {
            case Some(valueType) if valueType == ValueTypeString =>
              extractStringField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == ValueTypeLong =>
              extractLongField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == ValueTypeBytes =>
              extractBytesField(keyDownField).map(extractedValue => map.+(key.toLong -> extractedValue))

            case Some(valueType) if valueType == ValueTypeList =>
              extractListField(keyDownField).map(valueArray => map.+(key.toLong -> valueArray))

            case Some(valueType) if valueType == ValueTypeMap =>
              extractMapField(keyDownField, key).map(valueMap => map.+(key.toLong -> valueMap))

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

