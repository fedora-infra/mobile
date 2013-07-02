package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.os.Bundle
import android.view.{ LayoutInflater, View, ViewGroup }
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, ListView, TextView, Toast }

import spray.json._
import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import Datagrepper.JSONParsing._

import com.google.common.hash.Hashing

import java.text.SimpleDateFormat

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

    val newsfeed = findView(TR.newsfeed)
    val messages = getLatestMessages map { res =>
      HRF(res.messages.toString)
    }

    class NewsfeedAdapter(
      context: Context,
      resource: Int,
      items: Array[HRF.Result])
      extends ArrayAdapter[HRF.Result](context, resource, items) {

        override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
          val item = getItem(position)

          val layout = LayoutInflater.from(context)
            .inflate(R.layout.fedmsg_list_item, parent, false)
            .asInstanceOf[LinearLayout]

          val iconView = layout
            .findViewById(R.id.icon)
            .asInstanceOf[ImageView]

          // If there's a user associated with it, pull from gravatar.
          // Otherwise, just use the icon if it exists.
          if (item.usernames.length > 0) {
            val bytes = s"${item.usernames.head}@fedoraproject.org".getBytes("utf8")
            Cache.getGravatar(
              MainActivity.this,
              Hashing.md5.hashBytes(bytes).toString).onComplete { result =>
                result match {
                  case Success(gravatar) => {
                    runOnUiThread {
                      iconView.setImageBitmap(gravatar)
                    }
                  }
                  case _ =>
                }
            }
          } else {
            item.icon match {
              case Some(icon) => {
                Cache.getServiceIcon(MainActivity.this, icon, item.title) onSuccess { case icon =>
                  runOnUiThread {
                    iconView.setImageBitmap(icon)
                  }
                }
              }
              case None =>
            }
          }

          layout
            .findViewById(R.id.subtitle)
            .asInstanceOf[TextView]
            .setText(item.subtitle)

          layout
            .findViewById(R.id.timestamp)
            .asInstanceOf[TextView]
            .setText("- " + item.timestamp("fulldate") + " at " + item.timestamp("time"))

          layout
        }
    }

    messages onSuccess { case res =>
      // Yo dawg. I heard you like futures. So I put futures in your future.
      // For web-scale concurrency.
      res onSuccess { case hrfResult =>
        val adapter = new NewsfeedAdapter(
          this,
          android.R.layout.simple_list_item_1,
          hrfResult.toArray)
        runOnUiThread {
          newsfeed.tap { obj =>
            obj.setAdapter(adapter)
          }
        }
      }
      res onFailure { case utoh =>
        throw utoh
      }
    }
  }
}
