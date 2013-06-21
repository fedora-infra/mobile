package org.fedoraproject.mobile

import android.os.Bundle
import android.view.View
import android.widget.Toast

import com.google.android.gms.gcm.GoogleCloudMessaging

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global

class GCMDemoActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.gcm_demo_activity)

    future {
      val gcm = GoogleCloudMessaging.getInstance(this)
      gcm.register("677116861562")
    }.onSuccess { case result =>
      Toast.makeText(this, result + " FOO", Toast.LENGTH_LONG).show
    }
  }

  def register(view: View): Unit = {
    //Toast.makeText(this, registrationID, Toast.LENGTH_LONG).show
  }
}
