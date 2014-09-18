package org.fedoraproject.mobile

import android.graphics.Color
import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.io.{ Codec, Source }

object Status extends Webapi {
  val prodUrl = "http://status.fedoraproject.org/statuses.json"

  sealed trait StatusCondition
  case object Good      extends StatusCondition
  case object Minor     extends StatusCondition
  case object Major     extends StatusCondition
  case object Scheduled extends StatusCondition
  object StatusCondition {
    implicit val showStatusCondition: Show[StatusCondition] = new Show[StatusCondition] {
      override def show(t: StatusCondition) = t match {
        case Good      => Cord("good")
        case Minor     => Cord("minor")
        case Major     => Cord("major")
        case Scheduled => Cord("scheduled")
      }
    }

    // scalaz has no Read typeclass (because Read is a hack), but I don't want
    // to bring in all of atto for this, so for now, we just have this.
    def readOpt(status: String): Option[Status.StatusCondition] = status match {
      case "good"      => Some(Good)
      case "minor"     => Some(Minor)
      case "major"     => Some(Major)
      case "scheduled" => Some(Scheduled)
      case _           => None
    }
  }

  def colorFor(status: StatusCondition) = status match {
    case Good      => Color.parseColor("#009900")
    case Minor     => Color.parseColor("#ff6103")
    case Major     => Color.parseColor("#990000")
    case Scheduled => Color.parseColor("#ff6103")
  }

  case class StatusMetadata(
    message: String,
    name: String,
    status: Option[StatusCondition],
    url: String) // Can't use java.net.URL because Java is hilarious.

  case class StatusesResponse(
    global_info: String,
    global_status: String, // TODO: Status.StatusCondition
    global_verbose_status: String,
    services: Map[String, StatusMetadata])

  implicit def StatusCodecJson: CodecJson[StatusesResponse] =
    casecodec4(StatusesResponse.apply, StatusesResponse.unapply)("global_info", "global_status", "global_verbose_status", "services")

  implicit def StatusMetadataDecodeJson: DecodeJson[StatusMetadata] = {
    DecodeJson(c => for {
      message <- (c --\ "message").as[String]
      name    <- (c --\ "name").as[String]
      status  <- (c --\ "status").as[String]
      url     <- (c --\ "url").as[String]
    } yield StatusMetadata(message, name, StatusCondition.readOpt(status), url))
  }

 implicit def StatusMetadataEncodeJson: EncodeJson[StatusMetadata] =
   EncodeJson((x: StatusMetadata) =>
     Json(
       "message" -> jString(x.message),
       "name"    -> jString(x.name),
       "status"  -> x.status.map(s => jString(s.shows)).getOrElse(jNull),
       "url"     -> jString(x.url)
     ))

   def statuses(ctx: android.content.Context): Task[String] = {
     def perform(c: HttpURLConnection): Task[HttpURLConnection] = Task {
       c.setRequestMethod("GET")
       c.setRequestProperty("Content-Type", "application/json")
       c
     }

     for {
       c <- connection(ctx) >>= perform
     } yield (Source.fromInputStream(c.getInputStream)(Codec.UTF8).mkString)
   }
}
