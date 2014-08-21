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

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

object StatusColor {
  // TODO: This should most definitely be a sum type.
  def colorFor(status: String) = status match {
    case "good" => Some(Color.parseColor("#009900"))
    case "minor" | "scheduled" => Some(Color.parseColor("#ff6103"))
    case "major" => Some(Color.parseColor("#990000"))
    case _ => None
  }
}

case class StatusesResponse(
  global_info: String,
  global_status: String,
  global_verbose_status: String,
  services: Map[String, Map[String, String]])

class StatusFragment
  extends TypedFragment
  with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(activity)

  def onRefreshStarted(view: View): Unit = updateStatuses()

  implicit def StatusCodecJson: CodecJson[StatusesResponse] =
    casecodec4(StatusesResponse.apply, StatusesResponse.unapply)("global_info", "global_status", "global_verbose_status", "services")

  private def updateStatuses(): Unit = {
    val progress = findView(TR.progress)
    progress.setVisibility(View.VISIBLE)

    future {
      Source.fromURL("http://status.fedoraproject.org/statuses.json").mkString
    }.onComplete { result =>
      runOnUiThread(progress.setVisibility(View.GONE))
      result match {
        case Success(e) => {
          e.decodeEither[StatusesResponse].fold(
            err => Log.e("StatusFragment", err.toString),
            parsed => {
              val adapter = new StatusAdapter(
                activity,
                android.R.layout.simple_list_item_1,
                parsed.services.toArray.sortBy(_._2("name")))

              runOnUiThread(findView(TR.statuses).setAdapter(adapter))

              runOnUiThread {
                val globalInfoView = findView(TR.globalinfo)
                globalInfoView.setText(parsed.global_verbose_status)
                StatusColor.colorFor(parsed.global_status) map { c =>
                  globalInfoView.setBackgroundColor(c)
                }
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
