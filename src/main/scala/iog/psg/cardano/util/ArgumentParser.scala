package iog.psg.cardano.util

import scala.annotation.tailrec

class ArgumentParser(args: Array[String]) {

  lazy val (keyValues, params) = parse(args.toSeq, (Map.empty.withDefaultValue(None), List()))

  @tailrec
  private def parse(
                     args: Seq[String],
                     acc: (Map[String, Option[String]], List[String])
                   ): (Map[String, Option[String]], List[String]) = {

    args match {
      case Seq() => acc
      case Seq(head) =>
        require(head.startsWith("-"), "Mismatched parameters")
        (acc._1, head +: acc._2)
      case Seq(head, value, tail@_*) if value.startsWith("-") =>
        require(head.startsWith("-"), "Mismatched parameters")
        parse(value +: tail, (acc._1, head +: acc._2))
      case Seq(head, value, tail@_*) =>
        require(head.startsWith("-"), "Mismatched parameters")
        parse(tail, (acc._1 + (head -> Option(value)), acc._2))
    }

  }

  def apply(key: String): Option[String] = keyValues(key)

  def get(key: String): String = keyValues(key).getOrElse(throw new IllegalArgumentException(s"Key $key has no value"))

  def contains(key: String): Boolean = params.contains(key) || keyValues.keySet.contains(key)

  def noArgs: Boolean = args.isEmpty
}

