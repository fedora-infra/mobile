package org.fedoraproject.mobile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{ LayoutInflater, View, ViewGroup }

class RootFragment(layout: Int) extends Fragment {
  override def onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedInstanceState: Bundle): View =
    inflater.inflate(layout, container, false)
}
