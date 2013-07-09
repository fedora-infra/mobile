package org.fedoraproject.mobile

import android.net.Uri

import com.google.common.io.CharStreams

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }
import java.util.TimeZone

/** This is a general purpose way to interact with the Datagrepper API.
  *
  * Most of the time you will want to use [[HRF]] with this.
  *
  * For instance:
  * {{{
  * Datagrepper.query(List("delta" -> "1000")).map { r => JsonParser(r).convertTo[HRF.Response] }
  * }}}
  *
  * This class provides a high level abstraction and hides all of the HTTP
  * details behind a nice Scala API. All HTTP methods within return a Future
  * which, in normal circumstances, will hold the result of the query.
  */
object Datagrepper {
  val url = "https://apps.fedoraproject.org/datagrepper/raw/"
  val uri = Uri.parse(url).buildUpon

  case class Response(
    count: Int,
    pages: Int,
    messages: JsValue)

  private def constructURL(arguments: List[(String, String)]): String = {
    arguments foreach {
      case (key, value) =>
        uri.appendQueryParameter(key, value)
    }
    uri.build.toString
  }

  /** Returns a [[Future[String]]] of JSON after completing the query. */
  def query(arguments: List[(String, String)]) = {
    future {
      val connection = new URL(constructURL(arguments))
        .openConnection
        .asInstanceOf[HttpURLConnection]
      connection setRequestMethod "GET"
      CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
    }
  }

  object JSONParsing extends DefaultJsonProtocol {
    implicit val datagrepperResponse = jsonFormat(Response, "count", "pages", "raw_messages")
  }
}

object HRF {
  val url = "http://hrf.cloud.fedoraproject.org/all"

  case class Response(results: List[Result])

  case class Result(
    icon: Option[String], // TODO: Make this a Option[Future[Bitmap]] instead using BitmapCache.
    secondaryIcon: Option[String], // TODO: This too.
    link: Option[String],
    objects: List[String],
    packages: List[String],
    repr: String,
    subtitle: String,
    timestamp: Map[String, String],
    title: String,
    usernames: List[String] // TODO: When we have FAS integration, make this a List[FAS.User] or similar.
    )

  object JSONParsing extends DefaultJsonProtocol {
    implicit val messageFormat = jsonFormat(Result, "icon", "secondary_icon", "link", "objects", "packages", "repr", "subtitle", "timestamp", "title", "usernames")
    implicit val hrfResponse = jsonFormat(Response, "results")
  }

  import JSONParsing._

  def apply(messages: String, timezone: TimeZone): Future[List[Result]] =
    post(messages, timezone.getID) map { res =>
      JsonParser(res).convertTo[Response].results
    }

  def post(json: String, timezone: String): Future[String] = future {
    val connection = new URL(url + "?timezone=" + URLEncoder.encode(timezone, "utf8"))
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "POST"
    connection.setRequestProperty("Content-Type", "application/json");
    val os = new DataOutputStream(connection.getOutputStream)
    os.writeBytes(json)
    os.close
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }
}
