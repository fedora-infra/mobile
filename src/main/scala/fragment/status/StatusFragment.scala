package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.graphics.{ Color, PorterDuff }
import android.os.Bundle
import android.util.Log
import android.view.{ LayoutInflater, Menu, MenuItem, View, ViewGroup }
import android.widget.{ AdapterView, ArrayAdapter, LinearLayout, TextView, Toast }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

object Status {
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
      case "good"      => Some(Status.Good)
      case "minor"     => Some(Status.Minor)
      case "major"     => Some(Status.Major)
      case "scheduled" => Some(Status.Scheduled)
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
}

class StatusFragment
  extends TypedFragment
  with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(activity)

  def onRefreshStarted(view: View): Unit = updateStatuses()

  implicit def StatusCodecJson: CodecJson[Status.StatusesResponse] =
    casecodec4(Status.StatusesResponse.apply, Status.StatusesResponse.unapply)("global_info", "global_status", "global_verbose_status", "services")

  implicit def StatusMetadataDecodeJson: DecodeJson[Status.StatusMetadata] = {
    DecodeJson(c => for {
      message <- (c --\ "message").as[String]
      name    <- (c --\ "name").as[String]
      status  <- (c --\ "status").as[String]
      url     <- (c --\ "url").as[String]
    } yield Status.StatusMetadata(message, name, Status.StatusCondition.readOpt(status), url))
  }

 implicit def StatusMetadataEncodeJson: EncodeJson[Status.StatusMetadata] =
   EncodeJson((x: Status.StatusMetadata) =>
     Json(
       "message" -> jString(x.message),
       "name"    -> jString(x.name),
       "status"  -> x.status.map(s => jString(s.shows)).getOrElse(jNull),
       "url"     -> jString(x.url)
     ))

  private def updateStatuses(): Unit = {
    val progress = findView(TR.progress)
    progress.setVisibility(View.VISIBLE)

    // TODO: Rework 100% of this to use Task.
    // TODO: Stop pattern matching on the ADT constructors.
    // TODO: Stop using Source.fromURL
    Future {
      Source.fromURL("http://status.fedoraproject.org/statuses.json").mkString
    }.onComplete { result =>
      runOnUiThread(progress.setVisibility(View.GONE))
      result match {
        case Success(e) => {
          e.decodeEither[Status.StatusesResponse].fold(
            err => {
              Log.e("StatusFragment", err.toString)
              ()
            },
            parsed => {
              val adapter = new StatusAdapter(
                activity,
                android.R.layout.simple_list_item_1,
                parsed.services.toArray.sortBy(_._2.name))

              runOnUiThread(findView(TR.statuses).setAdapter(adapter))

              runOnUiThread {
                val globalInfoView = findView(TR.globalinfo)
                globalInfoView.setText(parsed.global_verbose_status)

                // TODO: StatusCondition instead of String.
                Status.StatusCondition.readOpt(parsed.global_status).map(c =>
                  globalInfoView.setBackgroundColor(Status.colorFor(c)))
              }
            }
          )
        }
        case Failure(e) =>
          runOnUiThread(Toast.makeText(activity, R.string.status_failure, Toast.LENGTH_LONG).show)
      }
    }
    runOnUiThread(refreshAdapter.setRefreshComplete)
    ()
  }

  override def onCreateView(i: LayoutInflater, c: ViewGroup, b: Bundle): View = {
    super.onCreateView(i, c, b)
    i.inflate(R.layout.status_activity, c, false)
  }

  override def onStart(): Unit = {
    super.onStart()
    val view = findView(TR.statuses)
    refreshAdapter.setRefreshableView(view, this)
    updateStatuses()
  }
}
