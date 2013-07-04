package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.view.{ Menu, MenuItem, View }

import spray.json._
import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source


import Datagrepper.JSONParsing._

import java.text.SimpleDateFormat
import java.util.TimeZone

class MainActivity extends NavDrawerActivity {

  private def getLatestMessages(): Future[Datagrepper.Response] = {
    Datagrepper.query(
      List(
        "delta" -> "604800",
        "order" -> "desc"
      )
    ) map { res =>
        JsonParser(res).convertTo[Datagrepper.Response]
      }
  }

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.main_activity)
    updateNewsfeed()
  }

  private def updateNewsfeed() {
    findView(TR.progress).setVisibility(View.VISIBLE)

    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages map { res =>
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
            val adapter = new FedmsgAdapter(
              this,
              android.R.layout.simple_list_item_1,
              hrfResult.toArray)
            runOnUiThread {
              newsfeed.tap { obj =>
                obj.setAdapter(adapter)
              }
            }
          }
        }
        res onFailure {
          case utoh =>
            throw utoh
        }
    }
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    super.onCreateOptionsMenu(menu)
    val inflater = getMenuInflater
    inflater.inflate(R.menu.main_activity, menu);
    true
  }

  override def onOptionsItemSelected(item: MenuItem) = {
    super.onOptionsItemSelected(item)
    item.getItemId match {
      case R.id.menu_refresh => {
        updateNewsfeed()
      }
      case _ =>
    }
    true
  }
}
