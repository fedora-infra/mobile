package org.fedoraproject.mobile

import android.util.Log

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

object Pure {
  def logV(tag: String, message: String) = Task {
    Log.v(tag, message)
    ()
  }
}
