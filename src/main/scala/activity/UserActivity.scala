package org.fedoraproject.mobile

import Implicits._
import util.Hashing

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.{ AbsListView, ArrayAdapter, Toast }

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.effect.IO

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import java.util.ArrayList // TODO: Do something about this.
import java.util.TimeZone

class UserActivity
  extends TypedActivity
  //with PullToRefreshAttacher.OnRefreshListener
  with util.Views {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  // The nickname is passed via the intent. If it's not there, something went
  // horribly wrong.
  private lazy val username: Option[String] =
    Option(getIntent.getExtras.getString("username"))


  /** Show a warning dialog that we this is only for demoing/testing. */
  private def showDemoWarning(): IO[Unit] = IO {
    val builder = new AlertDialog.Builder(this)
    builder.setNegativeButton("Right-o!", new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
      }
    })
    builder.setTitle("Hey there!")
    builder.setMessage("This is just a UI demo. The data is fake and is for UI testing only.")
    val dialog = builder.create
    dialog.show
  }

  /** Obtain a user's latest fedmsg stories.
    *
    * @todo Paginate/endless scrolling/etc.
  */
  private def getUserNewsfeed(/*since: Long*/): Task[String \/ List[HRF.Result]] =
    HRF(
      List(
        //"start"    -> (since + 1).toString,
        "start"    -> "0",
        "user"     -> "codeblock",
        "order"    -> "desc"
      ),
      TimeZone.getDefault)

  private def updateNewsfeed() {
  }

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.user_activity)

    showDemoWarning.unsafePerformIO

    val actionbar = getActionBar
    actionbar.setTitle(R.string.user_profile)

    // We don't have FAS integration yet (need OAuth) so all we can do is dream.
    // ...and fill in fake data to play with UI ideas.
    findView(TR.full_name).setText("Ricky Elrod")
    findView(TR.username).setText("codeblock")

    val profilePic = findView(TR.profile_pic)

    BitmapFetch.fromGravatarEmail("codeblock@fedoraproject.org").runAsync(_.fold(
      err => {
        Log.e("UserActivity", err.toString)
        ()
      },
      img => {
        runOnUiThread(profilePic.setImageBitmap(img))
        ()
      }
    ))

    findView(TR.badge_count).setText("43")
    findView(TR.fas_groups_count).setText("24")
    findView(TR.packages_count).setText("28")

    val newsfeed = findView(TR.user_newsfeed)
    val messages: Task[String \/ List[HRF.Result]] = getUserNewsfeed()
    messages.map(_.fold(
      err => {
        Log.e("UserActivity", "Error updating newsfeed: " + err)
        runOnUiThread(
          Toast.makeText(
            this,
            R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
      },
      res => {
        val arrayList = new ArrayList[HRF.Result]
        res.foreach(arrayList.add(_))
        val adapter = new FedmsgAdapter(
          this,
          android.R.layout.simple_list_item_1,
          arrayList)
        runOnUiThread(newsfeed.setAdapter(adapter))
      }
    ))
    ()
  }
}
