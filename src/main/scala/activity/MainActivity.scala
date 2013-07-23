package org.fedoraproject.mobile

import Datagrepper.JSONParsing._

import Implicits._

import android.os.Bundle
import android.util.Log
import android.view.{ Menu, MenuItem, View }
import android.widget.AbsListView.OnScrollListener
import android.widget.{ AbsListView, ArrayAdapter, Toast }

import spray.json._

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

import java.util.ArrayList // TODO: Do something about this.
import java.util.TimeZone

class MainActivity extends NavDrawerActivity with PullToRefreshAttacher.OnRefreshListener {

  private lazy val refreshAdapter = new PullToRefreshAttacher(this)

  private def getLatestMessages(before: Option[Long] = None): Future[Datagrepper.Response] = {
    // TODO: This is dirty like zebra.
    val query = List(
      "delta" -> "7200",
      "order" -> "desc"
    ) ::: (if (before.isDefined) List("start" -> before.get.toString) else Nil)

    Datagrepper.query(query) map { res =>
        JsonParser(res).convertTo[Datagrepper.Response]
      }
  }

  private def getMessagesSince(since: Long): Future[Datagrepper.Response] = {
    Datagrepper.query(
      List(
        "start" -> (since + 1).toString,
        "order" -> "desc"
      )
    ) map { res =>
        JsonParser(res).convertTo[Datagrepper.Response]
      }
  }

  def onRefreshStarted(view: View): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val newestTimestamp = newsfeed.getAdapter.getItem(0).asInstanceOf[HRF.Result].timestamp("epoch")
    val messages = getMessagesSince(newestTimestamp.replace(".0", "").toLong) map { res =>
      HRF(res.messages.toString, TimeZone.getDefault)
    }
    messages onComplete {
      case Success(res) => {
        res onComplete {
          case Success(hrfResult) => {
            val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
            runOnUiThread(hrfResult.reverse.foreach { result => adapter.insert(result, 0) })
            runOnUiThread(adapter.notifyDataSetChanged)
            runOnUiThread(refreshAdapter.setRefreshComplete)
          }
          case Failure(failure) => {
            runOnUiThread(Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show)
            Log.e("MainActivity", "Error refreshing: " + failure.toString)
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

  private def getNextPage(lastTimeStamp: Long): Unit = {
    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages(Option(lastTimeStamp)) map { res =>
      HRF(res.messages.toString, TimeZone.getDefault)
    }
    messages onSuccess {
      case res =>
        res onSuccess {
          case hrfResult => {
            val adapter = newsfeed.getAdapter.asInstanceOf[ArrayAdapter[HRF.Result]]
            runOnUiThread(hrfResult.foreach { result => adapter.add(result) })
            runOnUiThread(adapter.notifyDataSetChanged)
          }
        }
        res onFailure {
          case failure =>
            runOnUiThread {
              Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show
            }
        }
    }
  }

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.main_activity)
    updateNewsfeed()
    val newsfeed = findView(TR.newsfeed)

    refreshAdapter.setRefreshableView(newsfeed, this)

    newsfeed.setOnScrollListener(new OnScrollListener {
      def onScrollStateChanged(view: AbsListView, scrollState: Int) { /* ... */ }

      override def onScroll(view: AbsListView, firstVisible: Int, visibleCount: Int, totalCount: Int) {
        if (firstVisible + visibleCount == totalCount && totalCount != 0 && totalCount > visibleCount) {
          val last = view.getItemAtPosition(totalCount - 1).asInstanceOf[HRF.Result]
          getNextPage(last.timestamp("epoch").replace(".0", "").toLong)
        }
      }
    })
  }

  private def updateNewsfeed() {
    findView(TR.progress).setVisibility(View.VISIBLE)

    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages() map { res =>
      HRF(res.messages.toString, TimeZone.getDefault)
    }

    messages onSuccess {
      case res =>
        // Yo dawg. I heard you like futures. So I put futures in your future.
        // For web-scale concurrency.
        res onSuccess {
          case hrfResult => {
            runOnUiThread {
              findView(TR.progress).setVisibility(View.GONE)
            }
            val arrayList = new ArrayList[HRF.Result]
            hrfResult.foreach { result => arrayList.add(result) }
            val adapter = new FedmsgAdapter(
              this,
              android.R.layout.simple_list_item_1,
              arrayList)
            runOnUiThread(newsfeed.setAdapter(adapter))
          }
        }
        res onFailure {
          case failure =>
            runOnUiThread {
              Toast.makeText(this, R.string.newsfeed_failure, Toast.LENGTH_LONG).show
            }
        }
    }
  }
}
