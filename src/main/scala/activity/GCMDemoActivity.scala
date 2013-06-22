package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.view.View
import android.widget.Toast

import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.common.io.CharStreams

import scala.concurrent.{ Future, future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

class GCMDemoActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.gcm_demo_activity)

    future {
      val gcm = GoogleCloudMessaging.getInstance(this)
      gcm.register("677116861562")
    } onSuccess {
      case result =>
        sprungeRegistrationId(result) onSuccess {
          case sprungeURL =>
            runOnUiThread {
              Toast.makeText(this, sprungeURL, Toast.LENGTH_LONG).show
            }
        }
    }
  }

  // POSTing is so much fun with Java's stdlib.
  private def sprungeRegistrationId(id: String): Future[String] = future {
    val connection = new URL("http://sprunge.us/").openConnection.asInstanceOf[HttpURLConnection]
    connection setDoOutput true
    connection setRequestMethod "POST"
    val os = new DataOutputStream(connection.getOutputStream)
    val encodedID = URLEncoder.encode(id, "utf8")
    os.writeBytes(s"sprunge=${encodedID}")
    os.close
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }

  def register(view: View): Unit = {
    //Toast.makeText(this, registrationID, Toast.LENGTH_LONG).show
  }
}
