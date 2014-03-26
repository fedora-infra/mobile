package org.fedoraproject.mobile

import android.os.Bundle

class FedocalActivity extends TypedActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.main_activity)
  }
}
