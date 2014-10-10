package org.fedoraproject.mobile

import Implicits._

import android.app.{ Activity, NotificationManager, PendingIntent }
import android.content.{ BroadcastReceiver, Context, Intent }
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Log

import com.google.android.gms.gcm.GoogleCloudMessaging

import scalaz._, Scalaz._
import scalaz.effect._

sealed trait FMNMessage
case class RegistrationConfirmation(b: Bundle) extends FMNMessage
case class FedmsgNotification(b: Bundle) extends FMNMessage

sealed case class NotificationManagerCast[A](x: Object \/ A)
case object NotificationManagerCast {
  def unsafeCastNotificationManager(y: Object): NotificationManagerCast[NotificationManager] =
    if (y.isInstanceOf[NotificationManager])
      NotificationManagerCast(y.asInstanceOf[NotificationManager].right)
    else
      NotificationManagerCast(y.left)
}

class FedmsgReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = IO {
    val gcm = GoogleCloudMessaging.getInstance(context)

    val bundle: Bundle = intent.getExtras

    val nType: FMNMessage =
      if (bundle.containsKey("secret")) RegistrationConfirmation(bundle)
      else FedmsgNotification(bundle)

    setResultCode(Activity.RESULT_OK)
    sendNotification(context, nType)
    ()
  }.unsafePerformIO

  private def sendNotification(
    context: Context,
    nType: FMNMessage): IO[Unit] = IO {

     val notificationManager =
       NotificationManagerCast.unsafeCastNotificationManager(
         context.getSystemService(Context.NOTIFICATION_SERVICE))

    def createIntent(
      accepted: Option[Boolean],
      secret: String): IO[PendingIntent] = IO {
      val intent = accepted.cata(
        a =>
          new Intent(context, classOf[FedmsgConfirmationActivity])
            .putExtra("org.fedoraproject.mobile.accepted", a)
            .putExtra("org.fedoraproject.mobile.secret", secret)
            // http://stackoverflow.com/q/3127957/1106202
            // http://stackoverflow.com/a/3140371/1106202
            .setAction("FedmsgConfirmationActivity_" + System.currentTimeMillis),
          new Intent(context, classOf[FedmsgConfirmationActivity])
            .putExtra("org.fedoraproject.mobile.secret", secret)
            // http://stackoverflow.com/q/3127957/1106202
            // http://stackoverflow.com/a/3140371/1106202
            .setAction("FedmsgConfirmationActivity_" + System.currentTimeMillis)
      )
      PendingIntent.getActivity(context, 0, intent, 0)
    }

    val builder: IO[Option[NotificationCompat.Builder]] = nType match {
      case RegistrationConfirmation(bundle) =>
        for {
          acceptIntent  <- createIntent(Some(true), bundle.getString("secret", ""))
          rejectIntent  <- createIntent(Some(false), bundle.getString("secret", ""))
          neitherIntent <- createIntent(None, bundle.getString("secret", ""))
          compatBuilder <- IO {
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
                rejectIntent)
              .addAction(
                android.R.drawable.presence_online,
                context.getString(R.string.accept),
                acceptIntent)
              .setContentText(context.getString(R.string.fedmsg_confirmation_text))
              .setSmallIcon(R.drawable.fedoraicon)
              .setContentIntent(neitherIntent)
              .setAutoCancel(true)
          }
        } yield Some(compatBuilder)
      case FedmsgNotification(bundle) => {
        val hrf: IO[String \/ Option[HRF.Result]] = for {
          hrf <- HRF.fromJsonString(bundle.getString("message"))
        } yield hrf

        hrf.map(_.fold(
          error => {
            Log.e("FedmsgReceiver", "error parsing JSON")
            None
          },
          result => result.cata(
            r => Some(
              new NotificationCompat.Builder(context)
                .setContentTitle(r.subtitle)
                .setStyle(
                  new NotificationCompat
                  .BigTextStyle()
                  .bigText(r.subtitle)) // TODO: Fix when fmn.consumser#34 closes.
                .setContentText(r.subtitle)
                .setSmallIcon(R.drawable.fedoraicon)
                .setAutoCancel(true)),
            {
              Log.e("FedmsgReceiver", "Empty resultset from JSON")
              None
            }
          )
        ))
      }
    }
    builder.map(_.map(b => notificationManager.x.map(_.notify(1, b.build))))
    ()
  }
}
