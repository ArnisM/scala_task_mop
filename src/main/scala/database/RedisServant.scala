package database

import akka.actor.Actor
import akka.event.Logging
import akka.http.scaladsl.server.util.Tuple
import com.google.gson.{Gson, JsonObject, JsonParser}
import config.Configuration
import database.DatabaseO.config

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Arnis on 29.07.2017..
  */

class RedisServant(cache: RedisCache) extends Actor {

  val log = Logging(context.system, this)
  val redisCache: RedisCache = cache
  val gson: Gson = new Gson()
  def receive = {
    case Pair(s) =>

      if(s._1 == "delete") {
        redisCache.deleteNews(s._2.toString)
      }
      if(s._1 == "insert") {
        val x = gson.toJson(s._2)
        val t: JsonObject = gson.fromJson(x, classOf[JsonObject])
        val jelement = new JsonParser().parse(t.get("value").getAsString)
        val jobject = jelement.getAsJsonObject
        log.debug(s"${jobject}")
        redisCache.createNews(jobject)
      }
  }
}


