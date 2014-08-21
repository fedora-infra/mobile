package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast

import com.google.android.gms.gcm.GoogleCloudMessaging

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.io.{ Codec, Source }

class FedmsgConfirmationActivity extends TypedActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.fmn_confirmation_activity)

    val accepted: Option[Boolean] = Option(
      getIntent
        .getExtras
        .getBoolean("org.fedoraproject.mobile.accepted"))

    val fromDecisionNotification  = accepted.cata(
      decision => decide(decision),
      ??? // This probably means they tapped the notification
          // instead of answering it, or they are on an old
          // Android version. Show them the choices here
          // instead. TODO.
    )
    fromDecisionNotification.unsafePerformIO
  }

  private def finalizeDecision(
    accepted: Boolean,
    openid: String,
    apiKey: String,
    secret: String): Task[String] = delay {
    val connection =
      new URL(
        "https://apps.fedoraproject.org/notifications/confirm/" ++
          (if (accepted) "accept" else "reject") ++ "/" ++ openid ++
          "/" ++ secret ++ "/" ++ apiKey)
        .openConnection
        .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def decide(accepted: Boolean): IO[Unit] = {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val openid     = Option(sharedPref.getString("pref_fmn_openid", null))
    val apiKey     = Option(sharedPref.getString("pref_fmn_apikey", null))
    val extras     = getIntent.getExtras
    val secret     = extras.getString("org.fedoraproject.mobile.secret")

    if (openid.isEmpty || apiKey.isEmpty) {
      IO {
        Toast.makeText(
          this,
          R.string.fmn_no_username_or_api_key,
          Toast.LENGTH_LONG)
        .show
      }
    } else if (secret == "") {
      IO {
        Toast.makeText(
          this,
          R.string.fmn_no_secret,
          Toast.LENGTH_LONG)
        .show
      }
    } else {
      IO {
        val d: Task[String] =
          finalizeDecision(accepted, openid.get, apiKey.get, secret)
        d map {
          case _ => {
            Toast.makeText(
              this,
              R.string.fmn_stored,
              Toast.LENGTH_LONG).show
          }
        }
        ()
      }
    }
  }

  // Helper functions for button XML
  def accept(view: View) = decide(true)
  def reject(view: View) = decide(false)
}
