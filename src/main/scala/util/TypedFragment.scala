package org.fedoraproject.mobile

import android.app.Fragment
import android.content.Context
import android.view.View

import scalaz.concurrent.Task

trait TypedFragment extends Fragment with TypedActivityHolder {
  def findViewById(x: Int): View = getView.findViewById(x)
  def activity = getActivity
  def runOnUiThread(x: Runnable): Unit = activity.runOnUiThread(x)
  def findViewOpt[T](tr: TypedResource[T]) = Option(findView(tr))

  // Rather than passing context around manually everywhere, just make it
  // implicit here.
  implicit val ctxTask: Task[Context] =
    Task { getActivity.getApplicationContext }
}
