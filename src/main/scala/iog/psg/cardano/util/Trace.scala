package iog.psg.cardano.util

import io.circe.Encoder
import io.circe.syntax._

import java.io.{BufferedWriter, File, FileWriter}
import scala.util.Try

trait Trace extends AutoCloseable { parent =>

  def apply(s: String): Unit
  def apply[A](s: A)(implicit enc: Encoder[A]): Unit = apply(s.asJson.spaces2)

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
