package org.fedoraproject.mobile

package object Implicits {
  implicit def toRunnable[F](f: => F): Runnable = new Runnable() { def run() = { f } }

  /** A rough implementation of Ruby's "tap" method.
    *
    * This method lets you "tap" objects which don't return themselves when you
    * call their setters.
    *
    * For example, Android's [[android.widget.EditText]] doesn't return the
    * EditText when you call setHint(). setHint instead returns nothing (void).
    *
    * This "tap" method lets you work around this by doing something like:
    *
    * {{{
    * val et = new EditText(context).tap { obj =>
    *  obj.setHint(R.string.hint)
    *  obj.setSingleLine(true)
    * }
    * // et is now an EditText, but those setters have been invoked on it.
    * }}}
    */
  implicit def anyToTap[A](that: A) = new {
    def tap(f: (A) => Unit): A = {
      f(that)
      that
    }
  }
}
