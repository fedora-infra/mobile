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

class BadgesUserActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_user_activity)

    // The nickname is passed via the intent. If it's not there, something went
    // horribly wrong and fataling is probably the correct thing to do.
    val nickname = getIntent.getExtras.getString("nickname")

    Badges.query(s"/user/${nickname}/json") onComplete {
      case Success(res) => {
        val user = JsonParser(res).convertTo[Badges.User]
        val adapter = new BadgesUserAdapter(
          this,
          android.R.layout.simple_list_item_1,
          user.assertions.toArray)
          runOnUiThread(Option(findView(TR.user_badges)).map(_.setAdapter(adapter)))
      }
      case Failure(err) => {
        runOnUiThread(Toast.makeText(this, R.string.badges_user_failure, Toast.LENGTH_LONG).show)
        Log.e("BadgesUserActivity", err.toString)
      }
    }
  }
}
