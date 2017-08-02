package models
import org.joda.time.DateTime
/**
  * Created by Arnis on 29.07.2017..
  */

case class NewsItem(news_id: Option[Int] = None, title: String, content: String, createdDate: Option[String] = None, popularity: Int, tag: Array[String])
