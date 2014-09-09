package org.fedoraproject.mobile

import android.content.Context
import android.preference.PreferenceManager

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._

trait Webapi {
  def prodUrl: String
  def stagingUrl: Option[String] = None

  def appUrl(context: Context): Task[String] = Task {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    val staging = sharedPref.getBoolean("use_staging", false)
    staging.fold(
      stagingUrl.cata(identity, prodUrl),
      prodUrl
    )
  }
}
