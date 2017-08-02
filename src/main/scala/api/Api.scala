package api

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import database.RedisCache.connection
import database.{DatabaseO, RedisCache, RedisServant}

/**
  * Created by Arnis on 29.07.2017..
  */
object Api extends App with Service {

  override implicit val system = ActorSystem()
  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  logger.debug(s"HTTP: ${config.getString("akka.http.server.interface")}:${config.getString("akka.http.server.port")}")

  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  logger.debug(s"Redis: ${config.getString("news-api.redis.host")}:${config.getString("news-api.redis.port")}")

  override val redisCache = RedisCache()

  val redisServant = system.actorOf(Props(classOf[RedisServant], redisCache), "RedisServant")

  logger.debug(s"PG: ${config.getString("news-api.pg.host")}:${config.getString("news-api.pg.port")}")
  override val db = DatabaseO()

  logger.debug(s"Initialized all components, starting HTTP Server...")


  Http().bindAndHandle(routes, config.getString("akka.http.server.interface"), config.getInt("akka.http.server.port"))

}