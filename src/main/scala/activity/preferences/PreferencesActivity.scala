package org.fedoraproject.mobile

import android.os.Bundle
import android.preference.PreferenceActivity

// TODO: This should be a Fragment instead.

class PreferencesActivity extends PreferenceActivity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences)
    ()
  }
}
