package org.fedoraproject.mobile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{ LayoutInflater, View, ViewGroup }

class RootFragment(layout: Option[Int]) extends Fragment {
  def this() = this(None)
  override def onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedInstanceState: Bundle): View = layout match {
    case Some(layout) => inflater.inflate(layout, container, false)
    case _ => null
  }
}
