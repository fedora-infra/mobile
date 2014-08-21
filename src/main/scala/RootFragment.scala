package org.fedoraproject.mobile

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.{ LayoutInflater, View, ViewGroup }
import scalaz._, Scalaz._

class RootFragment(layout: Option[Int]) extends Fragment {
  def this() = this(None)
  override def onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedInstanceState: Bundle): View =
      layout.cata(
        layout => inflater.inflate(layout, container, false),
        null
      )
}
