package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import com.google.common.io.CharStreams

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import scala.io.Source

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

  implicit def ResponseCodecJson: CodecJson[Response] =
    casecodec1(Response.apply, Response.unapply)("results")

  implicit def ResultCodecJson: CodecJson[Result] =
    casecodec10(Result.apply, Result.unapply)("icon", "secondary_icon", "link", "objects", "packages", "repr", "subtitle", "timestamp", "title", "usernames")

  def apply(query: List[(String, String)], timezone: TimeZone): Promise[String \/ List[Result]] =
    promise {
      post(
        query.map(x =>
          s"${x._1}=${x._2}").mkString("&"), timezone.getID)
      .unsafePerformIO
      .decodeEither[Response].map(_.results)
    }

  def post(qs: String, timezone: String): IO[String] = IO {
    val postUrl: URL = new URL(url + "?timezone=" + URLEncoder.encode(timezone, "utf8") + "&" + qs)
    Log.v("HRF", "Beginning POST to " + postUrl)
    val connection: HttpURLConnection =
      postUrl
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    connection.setRequestProperty("Content-Type", "application/json");
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }
}
