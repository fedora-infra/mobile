package org.fedoraproject.mobile

import Implicits._
import util.Hashing

import scalaz._, Scalaz._
import scalaz.effect.IO

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgesUserActivity
  extends NavDrawerActivity
  with PullToRefreshAttacher.OnRefreshListener
  with util.Views {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  override def onPostCreate(bundle: Bundle): Unit = IO {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_user_activity)

    Option(getIntent.getExtras.getString("nickname")) match {
      case Some(nickname) => {
        val actionbar = getActionBar
        actionbar.setTitle(nickname)
        val email = s"${nickname}@fedoraproject.org"
        Cache.getGravatar(
          this,
          Hashing.md5(email)).onComplete { result =>
            result match {
              case Success(gravatar) =>
                runOnUiThread(actionbar.setIcon(new BitmapDrawable(getResources, gravatar)))
              case _ =>
            }
          }

        val badges = findView(TR.user_badges)
        refreshAdapter.setRefreshableView(badges, this)
      }
      case None => {
        Log.e(
          "BadgesUserActivity",
          "No nickname given. Spawned without an Intent somehow?")
        Toast.makeText(this, R.string.badges_user_failure, Toast.LENGTH_LONG).show
        finish() // Nuke the activity.
      }
    }
  }

  def onRefreshStarted(view: View): Unit = {
    updateBadges().unsafePerformIO
    runOnUiThread(refreshAdapter.setRefreshComplete)
  }

  def updateBadges(): IO[Unit] = IO {
    Option(getIntent.getExtras.getString("nickname")) match {
      case Some(nickname) => {
        Badges.user(nickname) map {
          case \/-(res) => {
            val adapter = new BadgesUserAdapter(
              this,
              android.R.layout.simple_list_item_1,
              res.assertions.toArray)
            findViewOpt(TR.user_badges).map(v => runOnUiThread(v.setAdapter(adapter)))
          }
          case -\/(err) => {
            runOnUiThread(Toast.makeText(this, R.string.badges_user_failure, Toast.LENGTH_LONG).show)
            Log.e("BadgesUserActivity", err.toString)
          }
        }
      }
      case None => {
        Log.e(
          "BadgesUserActivity",
          "No nickname given. Spawned without an Intent somehow?")
        Toast.makeText(this, R.string.badges_user_failure, Toast.LENGTH_LONG).show
        finish() // Nuke the activity.
      }
    }
  }
}
