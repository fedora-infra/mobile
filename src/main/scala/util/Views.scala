package org.fedoraproject.mobile.util

import org.fedoraproject.mobile.{ TypedActivity, TypedResource }

// This really isn't an activity, but it lets us access findView.
trait Views extends TypedActivity {
  /** A convenience method for finding views and needing them wrapped in Option.
    *
    * Many times views might go away (e.g., on orientation changes). We can
    * account for them disappearing by wrapping them in Option. This we can do
    * by either adding an implicit from TypedResource[T] to
    * Option[TypedResource[T]], or as an explicit method, which we chose to do
    * here for now. If this pattern is done extremely often, I would be fine
    * with moving this to be implicit.
    */
  def findViewOpt[T](tr: TypedResource[T]) = Option(findView(tr))
}
