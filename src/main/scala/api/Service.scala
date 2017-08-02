package api
import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.redis.RedisClient
import com.typesafe.config.Config
import database.{DatabaseO, RedisCache}
import models.NewsItem

import scala.concurrent.{ExecutionContextExecutor, Future}
/**
  * Created by Arnis on 29.07.2017..
  */
trait Service extends Protos {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit val redisCache: RedisCache
  val db: DatabaseO
  val redisServant : ActorRef
  def config: Config
  val logger: LoggingAdapter

  lazy val newsApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.news-api.host"), config.getInt("services.news-api.port"))

  def NewsAPIRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(newsApiConnectionFlow).runWith(Sink.head)

  def fetchNewsInfo(title: String): Future[Option[String]] = {
    val result: Future[Option[String]] = Future {
      redisCache.getNews(title)
    }
    result
  }
  def createNewsInfo(item: NewsItem): Future[Option[NewsItem]] = {
    db.createNewsItem(item)
  }
  def updateNewsInfo(item: NewsItem): Future[Option[NewsItem]] = {
    db.updateNewsItem(item)
  }
  def deleteNewsInfo(title: String): Future[Option[NewsItem]] = {
    db.deleteNewsItem(title)
  }
  val routes = logRequestResult("challenge") {
    pathPrefix("news") {
      (get & path(Segment)) { title =>
        complete {
          fetchNewsInfo(title).map(result =>
            HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, result.toString)))

        }
      } ~
        (post & entity(as[NewsItem])) { NewsItem =>
          complete {
            val createFuture = createNewsInfo(NewsItem)
            createFuture.map(result => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, result.toString)))
          }
        } ~
        (put & entity(as[NewsItem])) { NewsItem =>
          complete {
            logger.debug(s"$NewsItem")
            val updateFuture = updateNewsInfo(NewsItem)
            updateFuture.map(result => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, result.toString)))
          }
        } ~
        (delete & path(Segment)) { title =>
          complete {
            deleteNewsInfo(title).map(result =>
              HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, result.toString)))

          }
        }
    }
  }
}
