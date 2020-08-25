package iog.psg.cardano.util

import com.typesafe.config.{Config, ConfigFactory}

trait Configure {

  implicit val config = ConfigFactory.load()
  def config(name: String): Config = config.getConfig(name)
}

object ConfigureFactory extends Configure
