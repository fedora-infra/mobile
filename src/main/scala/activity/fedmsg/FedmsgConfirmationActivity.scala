package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast

import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.common.io.CharStreams

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

class FedmsgConfirmationActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.fmn_confirmation_activity)

    val accepted: Option[Boolean] = Option(
      getIntent
        .getExtras
        .getBoolean("org.fedoraproject.mobile.accepted"))

    val fromDecisionNotification  = accepted match {
      case Some(decision) => decide(decision)
      case None    => ??? // This probably means they tapped the notification
                          // instead of answering it, or they are on an old
                          // Android version. Show them the choices here
                          // instead. TODO.
    }
    fromDecisionNotification.unsafePerformIO
  }

  private def finalizeDecision(
    accepted: Boolean,
    openid: String,
    apiKey: String,
    secret: String): Promise[String] = promise {
    val connection =
      new URL(
        "https://apps.fedoraproject.org/notifications/confirm/" ++
          (if (accepted) "accept" else "reject") ++ "/" ++ openid ++
          "/" ++ secret ++ "/" ++ apiKey)
        .openConnection
        .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "GET"
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
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
        val d: Promise[String] =
          finalizeDecision(accepted, openid.get, apiKey.get, secret)
        d map {
          case _ => {
            Toast.makeText(
              this,
              R.string.fmn_stored,
              Toast.LENGTH_LONG)
            .show
          }
        }
      }
    }
  }

  // Helper functions for button XML
  def accept(view: View) = decide(true)
  def reject(view: View) = decide(false)
}
