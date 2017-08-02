package api
import models.NewsItem
import spray.json.DefaultJsonProtocol


/**
  * Created by Arnis on 29.07.2017..
  */
trait Protos extends DefaultJsonProtocol {

  implicit val newsItemFormat = jsonFormat6(NewsItem.apply)

}
