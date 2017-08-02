package config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by Arnis on 29.07.2017..
  */


trait Configuration {
  def config = Configuration.confInstance
}

object Configuration extends LazyLogging {
  val name = sys.env.getOrElse("SCALA_ENV", "application")

  logger.debug(s"Loading configuration for $name")
  val confInstance = ConfigFactory.load(name)

  implicit def toConfig(configuration: Configuration) = confInstance

}