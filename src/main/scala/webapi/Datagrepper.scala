package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import scala.io.{ Codec, Source }

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
      get(query.map(x => s"${x._1}=${x._2}").mkString("&"), timezone.getID)
      .unsafePerformIO
      .decodeEither[Response]
      .map(_.results)
    }

  def get(qs: String, timezone: String): IO[String] = IO {
    val getUrl: URL = new URL(url + "?timezone=" + URLEncoder.encode(timezone, "utf8") + "&" + qs)
    Log.v("HRF", "Beginning GET to " + getUrl)
    val connection: HttpURLConnection =
      getUrl
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    connection.setRequestProperty("Content-Type", "application/json")
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  /** Given a JSON string containing a fedmsg message, make it human readable.
    *
    * Send it to HRF, get back something we can display pieces of.
    */
  def fromJsonString(s: String): IO[String \/ Option[Result]] = IO {
    val qUrl: URL =
      new URL(
        url + "?timezone=" +
        URLEncoder.encode(TimeZone.getDefault.getID, "utf8"))
    val connection: HttpURLConnection =
      qUrl
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("POST")
    connection.setDoOutput(true)
    connection.setRequestProperty("Content-Type", "application/json")
    val os = new DataOutputStream(connection.getOutputStream)
    os.writeBytes(s)
    os.close
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
      .decodeEither[Response]
      .map(_.results.headOption)
  }
}

object Datagrepper {
  val url: URL = new URL("https://apps.fedoraproject.org/datagrepper/messagecount")

  case class Messagecount(messagecount: Long)

  implicit def MessagecountCodecJson: CodecJson[Messagecount] =
    casecodec1(Messagecount.apply, Messagecount.unapply)("messagecount")

  def messagecount(): Promise[String \/ Messagecount] = {
    def getMessageCount(): IO[String] = IO {
      val connection: HttpURLConnection =
        url
        .openConnection
        .asInstanceOf[HttpURLConnection]
      connection setRequestMethod "GET"
      connection.setRequestProperty("Content-Type", "application/json")
      Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
    }

    promise {
      getMessageCount().unsafePerformIO.decodeEither[Messagecount]
    }
  }
}
