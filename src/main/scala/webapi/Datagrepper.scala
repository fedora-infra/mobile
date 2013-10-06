package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import com.google.common.io.CharStreams

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }
import java.util.TimeZone

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

  def apply(query: List[(String, String)], timezone: TimeZone): Future[List[Result]] =
    post(
      query.map(x =>
        s"${x._1}=${x._2}").mkString("&"), timezone.getID).map(res =>
          JsonParser(res).convertTo[Response].results)

  def post(qs: String, timezone: String): Future[String] = future {
    Log.v("HRF", "Beginning POST")
    val connection = new URL(url + "?timezone=" + URLEncoder.encode(timezone, "utf8") + "&" + qs)
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "GET"
    connection.setRequestProperty("Content-Type", "application/json");
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }
}
