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

import scala.io.Source

import java.util.ArrayList // TODO: Do something about this.
import java.util.TimeZone

class FedmsgFeed
  extends NavDrawerActivity
  with PullToRefreshAttacher.OnRefreshListener
  with util.Views {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  private def getLatestMessages(before: Option[Long] = None): Promise[String \/ List[HRF.Result]] = {
    val query = List(
      "delta" -> "7200",
      "order" -> "desc"
    ) ::: (if (before.isDefined) List("start" -> before.get.toString) else Nil)

    HRF(query, TimeZone.getDefault)
  }

  private def getMessagesSince(since: Long): Promise[String \/ List[HRF.Result]] =
    HRF(
      List(
        "start" -> (since + 1).toString,
        "order" -> "desc"
      ),
      TimeZone.getDefault)

  def onRefreshStarted(view: View): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val newestItem = \/.fromTryCatch(newsfeed.getAdapter.getItem(0))
    newestItem match {
      case -\/(err) => updateNewsfeed()
      case \/-(item) => {
        val timestamp = item.asInstanceOf[HRF.Result].timestamp("epoch")
        val messages: Promise[String \/ List[HRF.Result]] = getMessagesSince(timestamp.replace(".0", "").toLong)
        messages map {
          case \/-(res) => {
            val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
            runOnUiThread(res.reverse.foreach(adapter.insert(_, 0)))
            runOnUiThread(adapter.notifyDataSetChanged)
            runOnUiThread(refreshAdapter.setRefreshComplete)
          }
          case -\/(err) => {
            runOnUiThread(Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
            Log.e("FedmsgFeed", "Error refreshing: " + err)
            runOnUiThread(refreshAdapter.setRefreshComplete)
          }
        }
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
    val messages: Promise[String \/ List[HRF.Result]] = getLatestMessages()
    val arrayList = new ArrayList[HRF.Result]
    val adapter = new FedmsgAdapter(
      this,
      android.R.layout.simple_list_item_1,
      arrayList)
    newsfeed.setAdapter(adapter)

    messages map {
      case -\/(err) => {
        Log.e("MainActivity", "Error updating newsfeed: " + err)
        runOnUiThread(
          Toast.makeText(
            this,
            R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
      }
      case \/-(res) => {
        res.foreach(arrayList.add(_))
        runOnUiThread(newsfeed.setAdapter(adapter))
      }
    }
  }
}
