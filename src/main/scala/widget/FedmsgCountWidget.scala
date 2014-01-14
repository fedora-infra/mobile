package org.fedoraproject.mobile

import Implicits._

import android.appwidget.{ AppWidgetManager, AppWidgetProvider }
import android.content.{ Context, Intent }
import android.util.Log
import android.widget.RemoteViews

import scalaz._, Scalaz._
import scalaz.concurrent.Promise

import java.text.SimpleDateFormat
import java.util.Date

class FedmsgCountWidgetProvider extends AppWidgetProvider {
  override def onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: Array[Int]): Unit = {
    appWidgetIds.foreach(n => {
      val mcPromise: Promise[String \/ Datagrepper.Messagecount] = Datagrepper.messagecount()
      mcPromise map {
        case -\/(err) => Log.e("FedmsgCountWidgetProvider", err)
        case \/-(mc) => {
          val views: RemoteViews = new RemoteViews(context.getPackageName, R.layout.widget_fedmsg_count)
          val time: String = new SimpleDateFormat("EEEEE, MMM d, yyyy h:mm a").format(new Date)
          views.setTextViewText(R.id.count, mc.messagecount.toString)
          views.setTextViewText(R.id.count_time, time.toString)
          appWidgetManager.updateAppWidget(n, views)
        }
      }
    })
  }
}
