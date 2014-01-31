package org.fedoraproject.mobile

import Implicits._

import android.app.{ Activity, NotificationManager, PendingIntent }
import android.content.{ BroadcastReceiver, Context, Intent }
import android.os.Bundle
import android.support.v4.app.NotificationCompat

import com.google.android.gms.gcm.GoogleCloudMessaging

import scalaz._, Scalaz._
import scalaz.effect._

sealed trait FMNMessage
case object RegistrationConfirmation extends FMNMessage
case object FedmsgNotification extends FMNMessage

class FedmsgReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = {
    val gcm = GoogleCloudMessaging.getInstance(context)

    val bundle: Bundle = intent.getExtras

    val nType: FMNMessage =
      if (bundle.containsKey("secret")) RegistrationConfirmation
      else FedmsgNotification

    setResultCode(Activity.RESULT_OK)
    sendNotification(nType, context, bundle).unsafePerformIO
  }

  private def sendNotification(
    nType: FMNMessage,
    context: Context,
    bundle: Bundle): IO[Unit] = IO {
    // Ugh, Object.
    val notificationManager =
      context
        .getSystemService(Context.NOTIFICATION_SERVICE)
        .asInstanceOf[NotificationManager]

    def createIntent(
      accepted: Option[Boolean],
      secret: String): PendingIntent = {
      val intent = accepted match {
        case Some(a: Boolean) =>
          new Intent(context, classOf[FedmsgConfirmationActivity])
            .putExtra("org.fedoraproject.mobile.accepted", a)
            .putExtra("org.fedoraproject.mobile.secret", secret)
            // http://stackoverflow.com/q/3127957/1106202
            // http://stackoverflow.com/a/3140371/1106202
            .setAction("FedmsgConfirmationActivity_" + System.currentTimeMillis)
        case None =>
          new Intent(context, classOf[FedmsgConfirmationActivity])
            .putExtra("org.fedoraproject.mobile.secret", secret)
            // http://stackoverflow.com/q/3127957/1106202
            // http://stackoverflow.com/a/3140371/1106202
            .setAction("FedmsgConfirmationActivity_" + System.currentTimeMillis)
      }

      PendingIntent.getActivity(context, 0, intent, 0)
    }

    val builder = nType match {
      case RegistrationConfirmation => {
        new NotificationCompat.Builder(context)
          .setContentTitle(
            context.getString(R.string.fedmsg_confirmation_title))
          .setStyle(
            new NotificationCompat
              .BigTextStyle()
              .bigText(context.getString(R.string.fedmsg_confirmation_text)))
          .addAction(
            android.R.drawable.presence_offline,
            context.getString(R.string.reject),
            createIntent(Some(false), bundle.getString("secret", "")))
          .addAction(
            android.R.drawable.presence_online,
            context.getString(R.string.accept),
            createIntent(Some(true), bundle.getString("secret", "")))
          .setContentText(context.getString(R.string.fedmsg_confirmation_text))
          .setSmallIcon(R.drawable.fedoraicon)
          .setContentIntent(
            createIntent(None, bundle.getString("secret", "")))
          .setAutoCancel(true)
        }
      case FedmsgNotification => new NotificationCompat.Builder(context) // TODO
    }
    notificationManager.notify(1, builder.build)
  }
}
