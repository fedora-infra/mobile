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

    def createIntent(accepted: Option[Boolean]): PendingIntent = {
      val intent = accepted match {
        case Some(a: Boolean) =>
          new Intent(context, classOf[FedmsgConfirmationActivity])
            .putExtra("accepted", a)
        case None => new Intent(context, classOf[FedmsgConfirmationActivity])
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
            createIntent(Some(false)))
          .addAction(
            android.R.drawable.presence_online,
            context.getString(R.string.accept),
            createIntent(Some(true)))
          .setContentText(context.getString(R.string.fedmsg_confirmation_text))
          .setSmallIcon(R.drawable.fedoraicon)
          .setContentIntent(createIntent(None))
        }
      case FedmsgNotification => ???
    }
    notificationManager.notify(1, builder.build)
  }
}
