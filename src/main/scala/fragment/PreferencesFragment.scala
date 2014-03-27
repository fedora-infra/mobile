package org.fedoraproject.mobile

import Implicits._

import android.os.Bundle
import android.preference.PreferenceFragment

import scalaz._, Scalaz._
import scalaz.effect.IO

class PreferencesFragment extends PreferenceFragment with TypedFragment {
  override def onCreate(savedInstanceState: Bundle): Unit = IO {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences);
  }.unsafePerformIO
}
