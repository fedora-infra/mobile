package org.fedoraproject.mobile

import android.content.Context
import android.preference.PreferenceManager

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._

import java.net.{ HttpURLConnection, URL, URLEncoder }

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

  def connection(context: Context): Task[HttpURLConnection] =
    for {
      url <- appUrl(context)
    } yield (new URL(url).openConnection.asInstanceOf[HttpURLConnection])
}
