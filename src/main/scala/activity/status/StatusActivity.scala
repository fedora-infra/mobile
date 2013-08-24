package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.graphics.{ Color, PorterDuff }
import android.os.Bundle
import android.view.{ LayoutInflater, Menu, MenuItem, View, ViewGroup }
import android.widget.{ AdapterView, ArrayAdapter, LinearLayout, TextView, Toast }

import spray.json._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

case class StatusesResponse(
  global_info: String,
  global_status: String,
  global_verbose_status: String,
  services: Map[String, Map[String, String]])

class StatusActivity extends NavDrawerActivity with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  def onRefreshStarted(view: View): Unit = updateStatuses()

  object StatusJsonProtocol extends DefaultJsonProtocol {
    implicit val f = jsonFormat4(StatusesResponse.apply)
  }

  import StatusJsonProtocol._

  // TODO: Move this somewhere.
  def getColorFor(status: String) =
    status match {
      case "good" => Some(Color.parseColor("#009900"))
      case "minor" | "scheduled" => Some(Color.parseColor("#ff6103"))
      case "major" => Some(Color.parseColor("#990000"))
      case _ => None
    }

  private def updateStatuses() {
    Option(findView(TR.progress)).map(_.setVisibility(View.VISIBLE))

    future {
      Source.fromURL("http://status.fedoraproject.org/statuses.json").mkString
    }.onComplete { result =>
      Option(findView(TR.progress)).map(v => runOnUiThread(v.setVisibility(View.GONE)))
      result match {
        case Success(e) => {
          val parsed = JsonParser(e).convertTo[StatusesResponse]

          val adapter = new StatusAdapter(
            this,
            android.R.layout.simple_list_item_1,
            parsed.services.toArray.sortBy(_._2("name")))

          runOnUiThread(Option(findView(TR.statuses)).map(_.setAdapter(adapter)))

          runOnUiThread {
            val globalInfoView = Option(findView(TR.globalinfo))
            globalInfoView match {
              case Some(globalInfoView) => globalInfoView.tap { obj =>
                obj.setText(parsed.global_verbose_status)
                getColorFor(parsed.global_status).map { c => obj.setBackgroundColor(c) }
              }
              case None =>
            }
          }

          runOnUiThread(refreshAdapter.setRefreshComplete)
        }
        case Failure(e) =>
          runOnUiThread(Toast.makeText(this, R.string.status_failure, Toast.LENGTH_LONG).show)
      }
    }
  }

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.status_activity)
    val view = findView(TR.statuses)
    refreshAdapter.setRefreshableView(view, this)
    updateStatuses()
  }
}
