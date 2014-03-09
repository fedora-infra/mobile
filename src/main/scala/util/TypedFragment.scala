package org.fedoraproject.mobile

import android.app.Fragment
import android.view.View

trait TypedFragment extends Fragment with TypedActivityHolder {
  def findViewById(x: Int): View = getView.findViewById(x)
  def activity = getActivity
  def runOnUiThread(x: Runnable): Unit = activity.runOnUiThread(x)
}
