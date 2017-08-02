package database
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives.complete
import com.google.gson.{Gson, JsonObject}
import com.redis._
import com.typesafe.scalalogging.LazyLogging
import config.Configuration
import database.RedisCache.{config, connection}

import scala.concurrent.Future
import scala.util.control.Exception
/**
  * Created by Arnis on 29.07.2017..
  */
trait RedisCache extends LazyLogging with Configuration {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val redisConnection: RedisClient

  val gson = new Gson
  def getNews(title: String): Option[String] = {
    val result: Option[String]  = {
      redisConnection.get(title.toLowerCase).map(result => result)
    }
    result
  }

  def getNews(title: String, async: Boolean): Future[Option[models.NewsItem]] = {
    val result: Future[Option[models.NewsItem]] = Future {
      redisConnection.get(title.toLowerCase).map(result => result.toString)
    }.map(result=>result).mapTo[Option[models.NewsItem]]
    result
  }

  def deleteNews(newsItem: models.NewsItem): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisConnection.del(newsItem.title.toLowerCase).map(result => result.toString)
    }
    result

  }
  def createNews(newsItem: models.NewsItem): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisConnection.set(newsItem.title.toLowerCase, gson.toJson(newsItem))
      redisConnection.get(newsItem.title.toLowerCase).map(result => result.toString)
    }
    result

  }
  def updateNews(newsItem: models.NewsItem): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisConnection.set(newsItem.title.toLowerCase, gson.toJson(newsItem))
      redisConnection.get(newsItem.title.toLowerCase).map(result => result.toString)
    }
    result
  }

  def deleteNews(title: String): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisConnection.del(title).map(result => result.toString)
    }
    result

  }
  def createNews(newsItem: JsonObject): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      logger.debug(s"${newsItem.get("title").getAsString.toLowerCase} => ${newsItem.toString}")
      redisConnection.set(newsItem.get("title").getAsString.toLowerCase, newsItem.toString)
      redisConnection.get(newsItem.get("title").getAsString.toLowerCase).map(result => result.toString)
    }
    result

  }
  def updateNews(newsItem: JsonObject): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisConnection.set(newsItem.get("title").getAsString.toLowerCase, gson.toJson(newsItem))
      redisConnection.get(newsItem.get("title").getAsString.toLowerCase).map(result => result.toString)
    }
    result
  }

}

object RedisCache extends Configuration {

  private val host = config.getString("news-api.redis.host")
  private val port = config.getInt("news-api.redis.port")


  val connection = new RedisClient(host, port)
  connection.connect
  if(connection.connected) {
    println("Connected to Redis...")
  }

  def apply(): RedisCache = new RedisCache {

    override implicit val redisConnection: RedisClient = connection

  }
}
