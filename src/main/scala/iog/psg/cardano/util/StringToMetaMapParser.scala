package iog.psg.cardano.util

import iog.psg.cardano.CardanoApi.TxMetadata
import iog.psg.cardano.CardanoApiMain.fail

import scala.util.{Failure, Success, Try}

object StringToMetaMapParser {

  def toMetaMap(mapAsStringOpt: Option[String]): Option[TxMetadata] = mapAsStringOpt.flatMap { mapAsStr =>

    if (mapAsStr.nonEmpty) {

      val parsedMap = mapAsStr
        .split(":")
        .grouped(2)
        .map {
          case Array(k, v) => k.toLongOption.toRight(k) -> v
        }

      val (invalidKeys, goodMap) = Try {
        parsedMap
          .foldLeft((Seq.empty[String], Seq.empty[(Long, String)])) {

            case ((errors, goodTuples), (Right(k), v)) =>
              (errors, goodTuples :+ (k -> v))

            case ((errors, goodTuples), (Left(k), _)) =>
              (errors :+ k, goodTuples)
          }

      } match {
        case Success(m) => m
        case Failure(_) =>
          fail(s"Map failed to parse into key value pairs, use format 'k:v:k1:v1:k2:v2' " +
            s"where all keys are numbers $mapAsStr")
      }
      if (invalidKeys.nonEmpty) {
        fail(s"I can't parse '${invalidKeys.mkString(", ")}' to map, use format 'k:v:k1:v1:k2:v2' where all keys are numbers")
      } else {
        Some(goodMap.toMap)
      }
    } else None

  }

}
