package org.fedoraproject.mobile

import android.os.Bundle

class MainActivity extends NavDrawerActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setUpNav(R.layout.main_activity)
  }
}
