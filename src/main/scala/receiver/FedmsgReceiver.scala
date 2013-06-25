package org.fedoraproject.mobile

import Implicits._

import android.app.{ Activity, NotificationManager, PendingIntent }
import android.content.{ BroadcastReceiver, Context, Intent }
import android.support.v4.app.NotificationCompat

import com.google.android.gms.gcm.GoogleCloudMessaging

class FedmsgReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = {
    val gcm = GoogleCloudMessaging.getInstance(context)

    gcm.getMessageType(intent) match {
      case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR =>
        sendNotification(context, s"Send error: ${intent.getExtras}")
      case GoogleCloudMessaging.MESSAGE_TYPE_DELETED =>
        sendNotification(context, s"Deleted messages on server: ${intent.getExtras}")
      case _ => sendNotification(context, s"Received: ${intent.getExtras}")
    }

    setResultCode(Activity.RESULT_OK)
  }

  private def sendNotification(context: Context, message: String): Unit = {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    val contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, classOf[MainActivity]), 0)
    val builder = new NotificationCompat.Builder(context)
      .setContentTitle("Fedmsg Notification")
      .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
      .setContentText(message)
      .setSmallIcon(R.drawable.fedoraicon)
      .setContentIntent(contentIntent)
    notificationManager.notify(1, builder.build)
  }
}
