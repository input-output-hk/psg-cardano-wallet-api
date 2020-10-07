package iog.psg.cardano.util

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}

trait CustomPatienceConfiguration extends Eventually {

  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))

}
