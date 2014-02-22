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

sealed abstract class FMNMessage
case class RegistrationConfirmation(b: Bundle) extends FMNMessage
case class FedmsgNotification(b: Bundle) extends FMNMessage

class FedmsgReceiver extends BroadcastReceiver {
  override def onReceive(context: Context, intent: Intent): Unit = IO {
    val gcm = GoogleCloudMessaging.getInstance(context)

    val bundle: Bundle = intent.getExtras

    val nType: FMNMessage =
      if (bundle.containsKey("secret")) RegistrationConfirmation(bundle)
      else FedmsgNotification(bundle)

    setResultCode(Activity.RESULT_OK)
    sendNotification(context, nType)
  }.unsafePerformIO

  private def sendNotification(
    context: Context,
    nType: FMNMessage): IO[Unit] = IO {

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

    val builder: IO[Option[NotificationCompat.Builder]] = nType match {
      case RegistrationConfirmation(bundle) =>
        IO {
          val compatBuilder = new NotificationCompat.Builder(context)
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
          Some(compatBuilder)
        }
      case FedmsgNotification(bundle) => {
        val hrf: IO[String \/ Option[HRF.Result]] = for {
          hrf <- HRF.fromJsonString(bundle.getString("message"))
        } yield hrf

        hrf map {
          case -\/(error) => {
            Log.e("FedmsgReceiver", "error parsing JSON")
            None
          }
          case \/-(result) =>
            result match {
              case None =>
                {
                  Log.e("FedmsgReceiver", "Empty resultset from JSON")
                  None
                }
              case Some(r) =>
                {
                  Some(
                    new NotificationCompat.Builder(context)
                      .setContentTitle(r.subtitle)
                      .setStyle(
                        new NotificationCompat
                          .BigTextStyle()
                          .bigText(r.repr))
                      .setContentText(r.repr)
                      .setSmallIcon(R.drawable.fedoraicon)
                      .setAutoCancel(true))
                }
            }
        }
      }
    }
    builder.map(_.map(b => notificationManager.notify(1, b.build)))
  }
}
