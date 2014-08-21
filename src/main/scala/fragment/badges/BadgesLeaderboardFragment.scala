package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.util.Log
import android.view.{ LayoutInflater, View, ViewGroup }
import android.widget.Toast

import scalaz._, Scalaz._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

class BadgesLeaderboardFragment
  extends TypedFragment
  with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(activity)

  override def onCreateView(i: LayoutInflater, c: ViewGroup, b: Bundle): View = {
    super.onCreateView(i, c, b)
    i.inflate(R.layout.badges_leaderboard_activity, c, false)
  }

  override def onStart(): Unit = {
    super.onStart()
    updateLeaderboard(true)
    val lb = findView(TR.leaderboard)
    refreshAdapter.setRefreshableView(lb, this)
  }

  def onRefreshStarted(view: View): Unit = {
    updateLeaderboard(false)
    runOnUiThread(refreshAdapter.setRefreshComplete)
  }

  def updateLeaderboard(showProgress: Boolean): Unit = {
    if (showProgress) {
      findViewOpt(TR.progress).map(_.setVisibility(View.VISIBLE))
    }

    Badges.leaderboard.runAsync(_.fold(
      err => {
        // Something bad happened when making the request
        runOnUiThread(Toast.makeText(activity, R.string.badges_lb_failure, Toast.LENGTH_LONG).show)
        Log.e("BadgesLeaderboardActivity", err.toString)
        ()
      },
      res => res.fold(
        err => {
          // Something bad happened when parsing the JSON response
          runOnUiThread(Toast.makeText(activity, R.string.badges_lb_failure, Toast.LENGTH_LONG).show)
          Log.e("BadgesLeaderboardActivity", err)
          ()
        },
        xs => {
          val adapter = new BadgesLeaderboardAdapter(
            activity,
            android.R.layout.simple_list_item_1,
            xs.toArray)

          if (showProgress) {
            findViewOpt(TR.progress).map(v => runOnUiThread(v.setVisibility(View.GONE)))
          }
          findViewOpt(TR.leaderboard).map(v => runOnUiThread(v.setAdapter(adapter)))
          ()
        }
      )
    ))
  }
}
