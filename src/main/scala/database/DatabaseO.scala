package database
import akka.actor.{ActorRef, ActorSystem, Props}
import api.Api.{redisCache, system}
import com.google.gson.{Gson, JsonArray, JsonObject, JsonParser}
import com.redis._
import com.typesafe.scalalogging.LazyLogging
import config.Configuration
import slick.jdbc
import slick.jdbc.PostgresProfile.api._
import slick.jdbc._

import scala.concurrent.Future

/**
  * Created by Arnis on 29.07.2017..
  */


trait DatabaseO extends LazyLogging with Configuration {
  import scala.concurrent.ExecutionContext.Implicits.global
  val redisServant : ActorRef
  implicit val dbConnection: Database
  val redisCache : RedisCache
  val intRedisCache: RedisCache
  val gson = new Gson
  val popularityServant: ActorRef
  val popularityCache: RedisCache
  def createNewsItem(newsItem: models.NewsItem): Future[Option[models.NewsItem]] = {

    val token = "$token$"

    //logger.debug(s"${gson.toJson(newsItem)}")
    val query = sql"SELECT create_news(#${token}#${gson.toJson(newsItem)}#${token});".as[String]//sql"INSERT INTO news_item (title, content, created_date, popularity, tags) VALUES (#${token}#${newsItem.title}#${token}, #${token}#${newsItem.content}#${token}, now(), #${newsItem.popularity}, #${newsItem.tag})".as[String]

    dbConnection.run(query).map(result => {
      redisServant.tell(("insert", result.headOption), redisServant)
      popularityServant.tell(result.headOption, popularityServant)
      result.headOption
    }).mapTo[Option[models.NewsItem]]
  }

  def updateNewsItem(newsItem: models.NewsItem): Future[Option[models.NewsItem]] = {

    var token = "$token$"
    val update_data = gson.toJson(newsItem)
    val t: JsonObject = gson.fromJson(update_data, classOf[JsonObject])
    t.remove("news_id")
    t.addProperty("news_id", newsItem.news_id.get)
    val query = sql"SELECT update_news(#${token}#${t}#${token});".as[String]

    dbConnection.run(query).map(result => {
      popularityServant.tell(result.headOption, popularityServant)
      redisServant.tell(("update", newsItem), redisServant)
      result.headOption
    }).mapTo[Option[models.NewsItem]]
  }
  def updateNewsItem(newsItem: String): Future[Option[models.NewsItem]] = {

    var token = "$token$"
    val query = sql"SELECT update_news(#${token}#${newsItem}#${token});".as[String]

    dbConnection.run(query).map(result => {
      //popularityServant.tell(result.headOption, popularityServant)
      //redisServant.tell(("update", newsItem), redisServant)
      result.headOption
    }).mapTo[Option[models.NewsItem]]
  }
  def deleteNewsItem(title: String): Future[Option[models.NewsItem]] = {
    val info: Future[Option[models.NewsItem]] = intRedisCache.getNews(title, async = true).map(result => {
      val x = gson.toJson(result)
      val t: JsonObject = gson.fromJson(x, classOf[JsonObject])
      val jelement = new JsonParser().parse(t.get("value").getAsString)
      val jobject = jelement.getAsJsonObject
      var token = "$token$"
      val query = sql"SELECT delete_news(#${token}#${jobject.get("title").getAsString}#${token});".as[String]
      dbConnection.run(query).map(result => {

        redisServant.tell(("delete", title), redisServant)
        result.headOption
      })
      result
    }).mapTo[Option[models.NewsItem]]
    info
  }

  def get2WeeksNews(): Future[Option[String]] = {
    val query = sql"SELECT get_news_last_2_weeks();".as[String]
    dbConnection.run(query).map(result => {
      result.headOption
    })
  }



  implicit class SQLActionBuilderExtensions(ab: SQLActionBuilder) {
    /**
      * Allows concatenation of the two raw SQl queries like so (disregard backslashes):
      *   sql"select \$a, \$b" ++ sql", \$c, \$d ..."
      */
    def ++(other: SQLActionBuilder) = SQLActionBuilder(
      ab.queryParts ++ other.queryParts, new SetParameter[Unit] {
        override def apply(u: Unit, pp: PositionedParameters): Unit = {
          ab.unitPConv.apply(u, pp)
          other.unitPConv.apply(u, pp)
        }
      }
    )
  }
}


object DatabaseO extends Configuration {

  private val host = config.getString("news-api.pg.host")
  private val port = config.getInt("news-api.pg.port")
  private val database = config.getString("news-api.pg.database")
  private val username = config.getString("news-api.pg.username")
  private val password = config.getString("news-api.pg.password")
  implicit val system = ActorSystem()
  val connection: jdbc.PostgresProfile.backend.DatabaseDef = Database.forURL(s"jdbc:postgresql://$host:$port/$database", username, password, null, "org.postgresql.Driver")
  //val redisConnection1 = new RedisClient(config.getString("news-api.redis.host"), config.getInt("news-api.redis.port"))

  val redisCache = RedisCache()
  val iRedisCache = RedisCache()
  val popCache = RedisCache()

  val rServant: ActorRef = system.actorOf(Props(classOf[RedisServant], redisCache), "RedisServant")

  val popServant: ActorRef = system.actorOf(Props(classOf[PopularityServant], popCache, apply()), "PopularityServant")

  def apply() :DatabaseO = new DatabaseO {
    override val redisServant: ActorRef = rServant
    override implicit val dbConnection:Database = connection
    override val redisCache: RedisCache = redisCache
    override val intRedisCache: RedisCache = iRedisCache
    override val popularityServant: ActorRef = popServant
    override val popularityCache: RedisCache = popCache

  }
}

class NewsItem(tag: Tag) extends Table[(Int, String, String, String, Int, String)](tag, "news_item") {

  def id : Rep[Int] = column[Int]("news_id", O.PrimaryKey, O.AutoInc, O.Unique)
  def title: Rep[String] = column[String]("title", O.Unique)
  def content: Rep[String] = column[String]("content")
  def created_date: Rep[String] = column[String]("created_date")
  def popularity: Rep[Int] = column[Int]("popularity")
  def tags: Rep[String] = column[String]("tag")
  def * = (id, title, content, created_date, popularity, tags)
}
