package org.fedoraproject.mobile

import android.os.Bundle

class FedocalActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.main_activity)
  }
}
