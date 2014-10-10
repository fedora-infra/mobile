package org.fedoraproject.mobile

import Implicits._

import android.app.Fragment
import android.os.{ Build, Bundle }
import android.preference.PreferenceManager
import android.util.Log
import android.view.{ LayoutInflater, Menu, MenuItem, View, ViewGroup }
import android.widget.AbsListView.OnScrollListener
import android.widget.{ AbsListView, ArrayAdapter, Toast }

import scalaz._, Scalaz._
import scalaz.concurrent.Task

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.io.Source

import java.util.ArrayList // TODO: Do something about this.

class FedmsgNewsfeedFragment
  extends TypedFragment
  with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(activity)

  private def getLatestMessages(before: Option[Long] = None): Task[String \/ List[HRF.Result]] = {
    val query = List(
      "delta" -> "7200",
      "order" -> "desc"
    ) ::: (if (before.isDefined) List("start" -> before.get.toString) else Nil)

    HRF(query)
  }

  private def getMessagesSince(since: Long): Task[String \/ List[HRF.Result]] = {
    HRF(
      List(
        "start" -> (since + 1).toString,
        "order" -> "desc"
      )
    )
  }

  def onRefreshStarted(view: View): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val newestItem = \/.fromTryCatchNonFatal(newsfeed.getAdapter.getItem(0))
    newestItem.fold(
      err => updateNewsfeed(),
      item => {
        val timestamp = item.asInstanceOf[HRF.Result].timestamp.toLong
        val messages: Task[String \/ List[HRF.Result]] = getMessagesSince(timestamp)
        messages.runAsync(_.fold(
          err => {
            // The Task threw an error (as opposed to the JSON parsing).
            // TODO: Abstract this.
            runOnUiThread(Toast.makeText(activity, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
            Log.e("FedmsgFeed", "Error refreshing: " + err)
            runOnUiThread(refreshAdapter.setRefreshComplete)
          },
          res => res.fold(
            err => {
              // JSON parsing threw an error.
              runOnUiThread(Toast.makeText(activity, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
              Log.e("FedmsgFeed", "Error refreshing: " + err)
              runOnUiThread(refreshAdapter.setRefreshComplete)
              ()
            },
            xs => {
              val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
              runOnUiThread(xs.reverse.foreach(adapter.insert(_, 0)))
              runOnUiThread(adapter.notifyDataSetChanged)
              runOnUiThread(refreshAdapter.setRefreshComplete)
            }
          )
        ))
      }
    )
    ()
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

  override def onStart(): Unit = {
    super.onStart()
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

  override def onCreateView(i: LayoutInflater, c: ViewGroup, b: Bundle): View = {
    super.onCreateView(i, c, b)
    i.inflate(R.layout.main_activity, c, false)
  }


  private def updateNewsfeed(): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val messages: Task[String \/ List[HRF.Result]] = getLatestMessages()
    val arrayList = new ArrayList[HRF.Result]
    val adapter = new FedmsgAdapter(
      activity,
      android.R.layout.simple_list_item_1,
      arrayList)
    newsfeed.setAdapter(adapter)

    messages.runAsync(_.fold(
      err => {
        // Something bad occurred in the Task
        Log.e("MainActivity", "Error updating newsfeed: " + err)
        runOnUiThread(
          Toast.makeText(
            activity,
            R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
      },
      res => res.fold(
        err => {
          // JSON parsing didn't work
          Log.e("MainActivity", "Error updating newsfeed: " + err)
          runOnUiThread(
            Toast.makeText(
              activity,
              R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
        },
        xs => {
          xs.foreach(arrayList.add(_))
          runOnUiThread(newsfeed.setAdapter(adapter))
        }
      )
    ))
  }
}
