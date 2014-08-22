package org.fedoraproject.mobile

import scala.language.implicitConversions

package object Implicits {
  implicit def toRunnable[F](f: => F): Runnable = new Runnable() {
    def run(): Unit = {
      f
      ()
    }
  }
}
