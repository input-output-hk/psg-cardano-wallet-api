package iog.psg.cardano.util

import java.io.{BufferedWriter, File, FileWriter}

import io.circe.Encoder

import scala.util.Try
import io.circe.syntax._

trait Trace extends AutoCloseable {

  parent =>

  implicit def s2Str[A](s: A)(implicit enc: Encoder[A]): String = s.asJson.spaces2
  def apply(s: String): Unit

  def withTrace(other: Trace): Trace = other match {
    case NoOpTrace => this
    case _ =>
      new Trace {

        override def apply(s: String): Unit = {
          parent.apply(s)
          other.apply(s)
        }

        override def close(): Unit = {
          Try(parent.close()).recover {
            case e => println(e)
          }
          Try(other.close()).recover {
            case e => println(e)
          }
        }
      }
  }
}


object NoOpTrace extends Trace {
  override def apply(s: String): Unit = ()
  override def close(): Unit = ()

  override def withTrace(other: Trace): Trace = other
}

object ConsoleTrace extends Trace {
  override def apply(s: String): Unit = println(s)
  override def close(): Unit = ()
}


class FileTrace(f: File) extends Trace {
  private val traceFile = new BufferedWriter(new FileWriter(f))

  override def apply(s: String): Unit = {
    traceFile.write(s)
    traceFile.newLine()
  }

  override def close(): Unit = {
    Try(traceFile.close()).recover {
      case e => println(e)
    }
  }
}
