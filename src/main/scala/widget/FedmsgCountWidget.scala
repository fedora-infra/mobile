package org.fedoraproject.mobile

import Implicits._

import android.appwidget.{ AppWidgetManager, AppWidgetProvider }
import android.content.{ Context, Intent }
import android.util.Log
import android.widget.RemoteViews

import scalaz._, Scalaz._
import scalaz.concurrent.Task

import java.text.{ NumberFormat, SimpleDateFormat }
import java.util.Date

class FedmsgCountWidgetProvider extends AppWidgetProvider {
  override def onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: Array[Int]): Unit = {
    appWidgetIds.foreach(n => {
      val mcTask: Task[String \/ Datagrepper.Messagecount] = Datagrepper.messagecount()
      mcTask.map(_.fold(
        err => Log.e("FedmsgCountWidgetProvider", err),
        mc => {
          val views: RemoteViews = new RemoteViews(context.getPackageName, R.layout.widget_fedmsg_count)
          val time: String = new SimpleDateFormat("h:mm a").format(new Date)
          val label: String = context.getString(R.string.fedmsg_count_time).format(time)
          val fedmsgCount: String = NumberFormat.getInstance.format(mc.messagecount)
          views.setTextViewText(R.id.count, fedmsgCount)
          views.setTextViewText(R.id.fedmsg_count_label, label)
          appWidgetManager.updateAppWidget(n, views)
        }
      ))
    })
  }
}
