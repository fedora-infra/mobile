package org.fedoraproject.mobile

import Badges.JSONParsing._

import Implicits._

import android.os.Bundle
import android.util.Log
import android.widget.Toast

import spray.json._

// import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgesLeaderboardActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_leaderboard_activity)

    Badges.query("/leaderboard/json") onComplete {
      case Success(res) => {
        val lb = JsonParser(res).convertTo[Badges.Leaderboard]
        val adapter = new BadgesLeaderboardAdapter(
          this,
          android.R.layout.simple_list_item_1,
          lb.leaderboard.toArray)
        runOnUiThread(Option(findView(TR.leaderboard)).map(_.setAdapter(adapter)))
      }
      case Failure(err) => {
        runOnUiThread(Toast.makeText(this, R.string.badges_lb_failure, Toast.LENGTH_LONG).show)
        Log.e("BadgesLeaderboardActivity", err.toString)
      }
    }
  }
}
