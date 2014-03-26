package org.fedoraproject.mobile

import Implicits._

import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Toast

import com.google.android.gms.gcm.GoogleCloudMessaging

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.io.{ Codec, Source }

/** This is where the user actually registers for FMN notifications.
  *
  * They hit a button which tells FMN "This is my API key, my oauth information,
  * and my GCM registration ID." Once FMN has this, it sends a notification
  * to us, which gets gets routed to FedmsgConfirmationActivity, which is where
  * we tell FMN whether or not they accepted.
  */
class FedmsgRegisterActivity extends TypedActivity {
  override def onPostCreate(bundle: Bundle): Unit = {
    super.onPostCreate(bundle)
    setContentView(R.layout.fmn_register_activity)

    val intent = getIntent
    val uri    = Option(intent.getData)
    val apiKey = uri.flatMap(x => Option(x.getQueryParameter("api_key")))
    val openid = uri.flatMap(x => Option(x.getQueryParameter("openid")))

    val storeAPIInfo: IO[Unit] =
      if (uri.isDefined) {
        if (apiKey.isEmpty || openid.isEmpty) {
          IO {
            Toast.makeText(
              this,
              R.string.fmn_error,
              Toast.LENGTH_LONG)
            .show
          }
        } else {
          IO {
            val sharedPrefEdit =
              PreferenceManager.getDefaultSharedPreferences(this).edit
            sharedPrefEdit.putString("pref_fmn_openid", openid.get)
            sharedPrefEdit.putString("pref_fmn_apikey", apiKey.get)
            sharedPrefEdit.commit()
            Toast.makeText(
              this,
              R.string.fmn_save_success,
              Toast.LENGTH_LONG)
            .show
          }
        }
      } else IO {}
    storeAPIInfo.unsafePerformIO
  }

  def register(view: View): Unit = {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    val openid     = Option(sharedPref.getString("pref_fmn_openid", null))
    val apiKey     = Option(sharedPref.getString("pref_fmn_apikey", null))

    val sendFMN =
      if (openid.isEmpty || apiKey.isEmpty)
        IO {
          Toast.makeText(
            this,
            R.string.fmn_no_username_or_api_key,
            Toast.LENGTH_LONG)
          .show
        }
      else
        IO {
          val registrationID: Promise[String] = getRegistrationID
          val fmnResponse = registrationID map {
            case id => sendIDToFMN(openid.get, apiKey.get, id)
          }
          fmnResponse map {
            case r => // TODO: Parse JSON response and do something with it.
          }
        }
    sendFMN.unsafePerformIO
    ()
  }

  private def sendIDToFMN(
    openid: String,
    apiKey: String,
    id: String): Promise[String] = promise {
     val connection =
      new URL(
        "https://apps.fedoraproject.org/notifications/link-fedora-mobile/" ++
          openid ++ "/" ++ apiKey ++ "/" ++ id)
        .openConnection
        .asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def getRegistrationID: Promise[String] = promise {
    val gcm = GoogleCloudMessaging.getInstance(this)
    gcm.register(getString(R.string.fmn_sender_id))
  }
}
