package org.fedoraproject.mobile

import Implicits._

import android.os.{ Build, Bundle }
import android.preference.PreferenceManager
import android.util.Log
import android.view.{ Menu, MenuItem, View }
import android.widget.AbsListView.OnScrollListener
import android.widget.{ AbsListView, ArrayAdapter, Toast }

import scalaz._, Scalaz._
import scalaz.concurrent.Promise

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

import java.util.ArrayList // TODO: Do something about this.
import java.util.TimeZone

class MainActivity
  extends NavDrawerActivity
  with PullToRefreshAttacher.OnRefreshListener
  with util.Views {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  private def getLatestMessages(before: Option[Long] = None): Future[String \/ List[HRF.Result]] = {
    val query = List(
      "delta" -> "7200",
      "order" -> "desc"
    ) ::: (if (before.isDefined) List("start" -> before.get.toString) else Nil)

    HRF(query, TimeZone.getDefault)
  }

  private def getMessagesSince(since: Long): Future[String \/ List[HRF.Result]] =
    HRF(
      List(
        "start" -> (since + 1).toString,
        "order" -> "desc"
      ),
      TimeZone.getDefault)

  def onRefreshStarted(view: View): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val newestTimestamp = newsfeed.getAdapter.getItem(0).asInstanceOf[HRF.Result].timestamp("epoch")
    val messages = getMessagesSince(newestTimestamp.replace(".0", "").toLong)
    messages onComplete {
      case Success(hrfResult) => {
        hrfResult match {
          case -\/(err) => Log.e("MainActivity", "Error refreshing: " + err)
          case \/-(res) => {
            val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
            runOnUiThread(res.reverse.foreach(adapter.insert(_, 0)))
            runOnUiThread(adapter.notifyDataSetChanged)
            runOnUiThread(refreshAdapter.setRefreshComplete)
          }
        }
      }
      case Failure(failure) => {
        runOnUiThread(Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
        Log.e("MainActivity", "Error refreshing: " + failure.toString)
        runOnUiThread(refreshAdapter.setRefreshComplete)
      }
    }
  }

  /*private def getNextPage(lastTimeStamp: Long): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages(Option(lastTimeStamp))
    messages onSuccess {
      case hrfResult => {
        val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
        runOnUiThread(hrfResult.foreach { result => adapter.add(result) })
        runOnUiThread(adapter.notifyDataSetChanged)
      }
    }
    messages onFailure {
      case failure =>
        runOnUiThread(
          Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
    }
  }*/

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.main_activity)

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val checkUpdates = sharedPref.getBoolean("check_updates", true)

    // If we're not in the emulator and the user hasn't disabled updates...
    if (checkUpdates) {
      val versionCompare: Promise[String \/ Boolean] = Updates.compareVersion(this)
      versionCompare map {
        case \/-(b) if b == true =>
          Log.e("MainActivity", "We're up to date!")
        case \/-(b) if b == false =>
          runOnUiThread(Updates.presentDialog(MainActivity.this))
        case -\/(err) =>
          Log.e("MainActivity", err.toString)
      }
    }

    updateNewsfeed()
    val newsfeed = findView(TR.newsfeed)

    refreshAdapter.setRefreshableView(newsfeed, this)

    /*newsfeed.setOnScrollListener(new OnScrollListener {
      def onScrollStateChanged(view: AbsListView, scrollState: Int) { /* ... */ }

      override def onScroll(view: AbsListView, firstVisible: Int, visibleCount: Int, totalCount: Int) {
        if (firstVisible + visibleCount == totalCount && totalCount != 0 && totalCount > visibleCount) {
          val last = view.getItemAtPosition(totalCount - 1).asInstanceOf[HRF.Result]
          getNextPage(last.timestamp("epoch").replace(".0", "").toLong)
        }
       }
    })*/
  }

  private def updateNewsfeed() {
    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages()
    messages onComplete {
      case Success(hrfResult) => {
        findViewOpt(TR.progress).map(v => runOnUiThread(v.setVisibility(View.GONE)))
        hrfResult match {
          case -\/(err) => Log.e("MainActivity", "Error updating newsfeed: " + err.toString)
          case \/-(res) => {
            val arrayList = new ArrayList[HRF.Result]
            res.foreach(arrayList.add(_))
            val adapter = new FedmsgAdapter(
              this,
              android.R.layout.simple_list_item_1,
              arrayList)
            runOnUiThread(newsfeed.setAdapter(adapter))
          }
        }
      }
      case Failure(err) => {
      findViewOpt(TR.progress).map(v => runOnUiThread(v.setVisibility(View.GONE)))
        runOnUiThread(
          Toast.makeText(
            this,
            R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
        Log.e("MainActivity", "Error updating newsfeed: " + err.toString)
      }
    }
  }
}
