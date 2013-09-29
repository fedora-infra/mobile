package org.fedoraproject.mobile

import Badges.JSONParsing._

import Implicits._

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import spray.json._

import com.google.common.hash.Hashing

// import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgesUserActivity extends NavDrawerActivity with util.Views {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_user_activity)

    // The nickname is passed via the intent. If it's not there, something went
    // horribly wrong and fataling is probably the correct thing to do.
    val nickname = getIntent.getExtras.getString("nickname")

    val actionbar = getActionBar
    actionbar.setTitle(nickname)

    // TODO: Include rank one day?
    //actionbar.setSubtitle(pkg.summary)

    val bytes = s"${nickname}@fedoraproject.org".getBytes("utf8")
    Cache.getGravatar(
      this,
      Hashing.md5.hashBytes(bytes).toString).onComplete { result =>
        result match {
          case Success(gravatar) =>
            runOnUiThread(actionbar.setIcon(new BitmapDrawable(getResources, gravatar)))
          case _ =>
        }
      }

    Badges.query(s"/user/${nickname}/json") onComplete {
      case Success(res) => {
        val user = JsonParser(res).convertTo[Badges.User]
        val adapter = new BadgesUserAdapter(
          this,
          android.R.layout.simple_list_item_1,
          user.assertions.toArray)
        findViewOpt(TR.user_badges).map(v => runOnUiThread(v.setAdapter(adapter)))
      }
      case Failure(err) => {
        runOnUiThread(Toast.makeText(this, R.string.badges_user_failure, Toast.LENGTH_LONG).show)
        Log.e("BadgesUserActivity", err.toString)
      }
    }
  }
}
