package database

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.event.Logging
import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.google.gson.{Gson, JsonElement, JsonObject, JsonParser}
import com.typesafe.config.Config
import config.Configuration
import slick.jdbc

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Arnis on 01.08.2017..
  */

class PopularityServant(cache: RedisCache, db: DatabaseO) extends Actor with Configuration {
  import scala.concurrent.ExecutionContext.Implicits.global
  val log = Logging(context.system, this)
  val redisCache: RedisCache = cache
  val databaseO : DatabaseO = db
  val gson: Gson = new Gson()
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http = Http(context.system)
  val scheduler = context.system.scheduler
  val task = new Runnable { def run() {
    db.get2WeeksNews().map(result => {
      val x = gson.toJson(result)
      val t: JsonObject = gson.fromJson(x, classOf[JsonObject])
      val jelement = new JsonParser().parse(t.get("value").getAsString)
      val jarray = jelement.getAsJsonArray
      jarray.forEach(j => {

        val a = j.getAsJsonObject
        val response: Future[HttpResponse] =
          http.singleRequest(HttpRequest(uri = news_raw_uri, method = HttpMethods.GET))

        val result = response.map {
          case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            Unmarshal(entity).to[String]

          case x => s"Unexpected status code ${x.status}"

        }.map(r => {

          val x1 = gson.toJson(r)
          val t1: JsonObject = gson.fromJson(x1, classOf[JsonObject])

          val jelement1 = new JsonParser().parse(t1.get("a").getAsString)
          val jobject1 = jelement1.getAsJsonObject
          val articles = jobject1.getAsJsonArray("articles")
          val titleParts:Array[String] = a.get("title").getAsString.split(" ")
          titleParts.foreach(t => {
            articles.forEach(j => {
              val r = j.getAsJsonObject
              if(r.get("title").getAsString.toLowerCase.contains(t.toLowerCase)) {
                a.addProperty("popularity", (a.get("popularity").getAsInt+1))
                redisCache.updateNews(a)
                databaseO.updateNewsItem(a.toString)
              }

            })
          })

          r
        })
      })
    })
  } }
  implicit val executor = context.system.dispatcher

  val news_uri: String = config.getString("news-api.third-party.uri")
  log.debug(s"Loaded url config: [$news_uri]")
  val api_key: String = config.getString("news-api.third-party.api-key")

  val news_raw_uri: String = config.getString("news-api.third-party.raw-uri")

  scheduler.schedule(
    initialDelay = Duration(5, TimeUnit.SECONDS),
    interval = Duration(6, TimeUnit.HOURS),
    runnable = task)

  def receive = {

    case s =>
      val x = gson.toJson(s)
      val t: JsonObject = gson.fromJson(x, classOf[JsonObject])
      val jelement = new JsonParser().parse(t.get("value").getAsString)
      val jobject = jelement.getAsJsonObject
      log.debug(s"${jobject}")
      val titleParts:Array[String] = jobject.get("title").getAsString.split(" ")

      val response: Future[HttpResponse] =
    http.singleRequest(HttpRequest(uri = news_raw_uri, method = HttpMethods.GET))

    val result = response.map {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Unmarshal(entity).to[String]

      case x => s"Unexpected status code ${x.status}"

    }.map(r => {

      val x1 = gson.toJson(r)
      val t1: JsonObject = gson.fromJson(x1, classOf[JsonObject])
      val jelement1 = new JsonParser().parse(t1.get("a").getAsString)
      val jobject1 = jelement1.getAsJsonObject
      val articles = jobject1.getAsJsonArray("articles")
      titleParts.foreach(t => {
        articles.forEach(j => {
          val a = j.getAsJsonObject
          if(a.get("title").getAsString.toLowerCase.contains(t.toLowerCase)) {
            jobject.addProperty("popularity", (jobject.get("popularity").getAsInt+1))
          }
        })
      })
      redisCache.createNews(jobject)
      databaseO.updateNewsItem(jobject.toString)
      r
    })

  }
}

