package iog.psg.cardano.experimental.cli.util

import scala.sys.process._
import scala.util.Try

trait ProcessResult[A] {
  def apply(process: ProcessBuilder): A
  def map[B](f: A => B): ProcessResult[B] = a => f(apply(a))
}

object ProcessResult {
  def apply[T](implicit PR: ProcessResult[T]): ProcessResult[T] = PR

  implicit val LazyListOfStrings: ProcessResult[LazyList[String]] = _.lazyLines
  implicit val ListOfStrings: ProcessResult[List[String]] = ProcessResult[LazyList[String]].map(_.toList)
  implicit val String: ProcessResult[String] = ProcessResult[LazyList[String]].map(_.mkString)
  implicit val Int: ProcessResult[Int] = _.!
  implicit val Unit: ProcessResult[Unit] = _.!!

  implicit def either[T: ProcessResult]: ProcessResult[Either[Throwable, T]] = {
    process => Try(ProcessResult[T].apply(process)).toEither
  }

  implicit def eitherString[T: ProcessResult]: ProcessResult[Either[String, T]] = {
    either[T].map(_.left.map(_.getMessage))
  }
}