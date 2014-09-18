package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.{ LayoutInflater, Menu, MenuItem, View, ViewGroup }
import android.widget.{ AdapterView, ArrayAdapter, LinearLayout, TextView, Toast }

import argonaut._, Argonaut._
import scalaz._, Scalaz._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.io.Source

class StatusFragment
  extends TypedFragment
  with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(activity)

  def onRefreshStarted(view: View): Unit = updateStatuses()

  private def updateStatuses(): Unit = {
    val progress = findView(TR.progress)
    progress.setVisibility(View.VISIBLE)

    Status.statuses(getActivity.getApplicationContext).runAsync(_.fold(
      err =>
        runOnUiThread(Toast.makeText(activity, R.string.status_failure, Toast.LENGTH_LONG).show),
      res => {
        runOnUiThread(progress.setVisibility(View.GONE))
        res.decodeEither[Status.StatusesResponse].fold(
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
        runOnUiThread(refreshAdapter.setRefreshComplete)
      }
    ))
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
