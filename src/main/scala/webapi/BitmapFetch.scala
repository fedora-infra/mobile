package org.fedoraproject.mobile

import util.Hashing

import Pkgwat._

import android.graphics.{ Bitmap, BitmapFactory }
import android.net.Uri
import android.util.Log

import scalaz._, Scalaz._
import scalaz.concurrent.Task

import java.net.{ HttpURLConnection, URL, URLEncoder }

object BitmapFetch {
  def fromURL(imgUrl: String): Task[Bitmap] = Task {
    val url = new URL(imgUrl)
    val connection = url.openConnection
    connection setUseCaches true
    val is = connection.getInputStream
    BitmapFactory.decodeStream(is)
  }

  def fromGravatarEmail(
    email: String,
    size: Int = 64,
    default: String = "https://fedoraproject.org/static/images/fedora_infinity_64x64.png"): Task[Bitmap] =
      Task {
        val defaultEncoded = URLEncoder.encode(default, "utf8")
        val md5sum = Hashing.md5(email)
        val connection =
          new URL(s"https://seccdn.libravatar.org/avatar/$md5sum?s=$size&d=$defaultEncoded")
          .openConnection
          .asInstanceOf[HttpURLConnection]
        connection setUseCaches true
        connection setInstanceFollowRedirects true
        val is = connection.getInputStream
        BitmapFactory.decodeStream(is)
      }

  def fromPackage(p: FedoraPackage) = Task {
    val icon = URLEncoder.encode(p.icon, "utf8")
    val connection =
       new URL(s"https://apps.fedoraproject.org/packages/images/icons/${icon}.png")
       .openConnection
       .asInstanceOf[HttpURLConnection]
     connection setUseCaches true
     connection setInstanceFollowRedirects true
     val is = connection.getInputStream
     BitmapFactory.decodeStream(is)
  }
}
