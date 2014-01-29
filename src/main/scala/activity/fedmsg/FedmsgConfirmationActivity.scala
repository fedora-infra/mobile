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

    val intent = getIntent

    val accepted: Option[Boolean] =
      if (intent.hasExtra("action"))
        // The second argument to getBooleanExtra can be anything, since we
        // already check that it exists and won't hit this case if it doesn't.
        Some(intent.getBooleanExtra("action", false))
      else
        None

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
    username: String,
    apiKey: String,
    secret: String): IO[String] = IO {
    val openid = username + ".id.fedoraproject.org"
    val connection =
      new URL(
        "https://apps.fedoraproject.org/notifications/confirm/" ++
          (if (accepted) "accept" else "reject") ++ "/" ++ secret ++ "/" ++
          apiKey)
        .openConnection
        .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "GET"
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }

  def decide(accepted: Boolean): IO[Option[String]] = {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val username   = Option(sharedPref.getString("pref_fas_username", null))
    val apiKey     = Option(sharedPref.getString("pref_fmn_apikey", null))
    val intent     = getIntent
    val secret     = intent.getStringExtra("secret")

    if (username.isEmpty || apiKey.isEmpty) {
      IO {
        Toast.makeText(
          this,
          R.string.fmn_no_username_or_api_key,
          Toast.LENGTH_LONG)
        .show
        None
      }
    } else IO {
      Option(
        finalizeDecision(
          accepted,
          username.get,
          apiKey.get,
          secret)
        .unsafePerformIO) // TODO: Yuck.
    }
  }

  // Helper functions for button XML
  def accept(view: View) = decide(true)
  def reject(view: View) = decide(false)
}
