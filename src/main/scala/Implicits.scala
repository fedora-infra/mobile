package org.fedoraproject.mobile

package object Implicits {
  implicit def toRunnable[F](f: => F): Runnable = new Runnable() { def run() = f }
}
