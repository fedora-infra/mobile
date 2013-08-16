package org.fedoraproject.mobile

import android.content.Context
import android.graphics.{ Bitmap, BitmapFactory }
import android.util.Log

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.{ File, BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream, InputStream }
import java.net.{ HttpURLConnection, URL, URLEncoder }

/** A very general purpose cache outline. */
trait Cache[A] {
  val context: Context
  val directoryName: String
  val fileName: String

  def isHit: Boolean
  def hit(): A
  def miss(): Unit
  def get(): Future[A]
}

/** A slightly less general purpose cache outline which requires a URL.
  *
  * This trait is for classes which provide access to the cache for static
  * resources obtained from the web.
  */
trait WebCache[A] extends Cache[A] {
  val connection: HttpURLConnection
}

/** Controls access to the cache for images.
  *
  * This will download the image from the url given, if a path in the cache
  * directory with the same name doesn't exist already. Then it will return
  * the (now-cached, if necessary) cached image from the cache. Yo dawg.
  *
  * @param context The Android context on which to act.
  * @param url A [[java.net.URL]] that is the image's URL.
  * @param directoryName The directory in cache to store the image in.
  * @param fileName The name of the file to store in the cache.
  */
class BitmapCache(
  val context: Context,
  val connection: HttpURLConnection,
  val directoryName: String,
  val fileName: String,
  localCacheExpire: Int = 0) extends WebCache[Bitmap] {

  private val directory = new File(context.getCacheDir.toString + "/" + directoryName)
  directory.mkdir

  private lazy val file = new File(directory.toString + "/" + fileName)

  /** Checking for 304 is relatively cheap, but we don't want to do it every
    * single time, because it could be hundreds of times in a row, e.g. in
    * a ListView of package search results. So we check 304, but only if we
    * haven't gotten the image since localCacheExpire seconds ago.
    */
  lazy val isHit = {
    if ((System.currentTimeMillis / 1000.0) - (file.lastModified / 1000.0) > localCacheExpire) {
      connection.setIfModifiedSince(file.lastModified)
      connection.getResponseCode == 304
    } else {
      Log.v("BitmapCache", s"[FORCED-HIT] $file")
      true
    }
  }

  def hit(): Bitmap = {
    Log.v("BitmapCache", s"[HIT] $file")
    val inputStream = new BufferedInputStream(new FileInputStream(file))
    BitmapFactory.decodeStream(inputStream)
  }

  def miss(): Unit = {
    Log.v("BitmapCache", s"[MISS] ${connection.getURL}")
    if (file.exists) file.delete()
    val urlStream = connection.getInputStream
    val outputStream = new BufferedOutputStream(new FileOutputStream(file))
    Iterator
      .continually(urlStream.read)
      .takeWhile(-1 !=)
      .foreach(outputStream.write)
    outputStream.flush
  }

  def get(): Future[Bitmap] = {
    future {
      if (!isHit) miss()
      hit()
    }
  }
}

object Cache {
  /** Returns a [[Future[Bitmap]]] containing the icon for a package.
    *
    * @param context The context on which to act.
    * @param path The name of the image on the server (and in cache).
    */
  def getPackageIcon(context: Context, path: String): Future[Bitmap] =
    new BitmapCache(
      context,
      new URL(s"https://apps.fedoraproject.org/packages/images/icons/${path}.png").openConnection.asInstanceOf[HttpURLConnection],
      "package_icons",
      path,
      3600 * 12).get()

  /** Returns a [[Future[Bitmap]]] containing the icon for a service.
    *
    * @param context The context on which to act.
    * @param path The name of the image on the server (and in cache).
    */
  def getServiceIcon(context: Context, iconUrl: String, topic: String): Future[Bitmap] =
    new BitmapCache(
      context,
      new URL(iconUrl).openConnection.asInstanceOf[HttpURLConnection],
      "service_icons",
      topic,
      3600 * 24 * 7).get()

  /** Returns a [[Future[Bitmap]]] containing the image for a badge.
    *
    * @param context The context on which to act.
    * @param iconUrl The complete URL at which the badge image resides.
    */
  def getBadgeImage(context: Context, iconUrl: String, name: String): Future[Bitmap] =
    new BitmapCache(
      context,
      new URL(iconUrl).openConnection.asInstanceOf[HttpURLConnection],
      "badges",
      name,
      3600 * 24 * 7).get()

  /** Returns a [[Future[Bitmap]]] containing a Gravatar.
    *
    * @param context The context on which to act.
    * @param md5sum A md5sum of the email address to look up.
    * @param size How big the image should be in Y x Y pixels.
    * @param default A URL for a default image if one isn't found.
    */
  def getGravatar(
    context: Context,
    md5sum: String,
    size: Int = 64,
    default: String = "https://fedoraproject.org/static/images/fedora_infinity_64x64.png"): Future[Bitmap] = {
    val defaultEncoded = URLEncoder.encode(default, "utf8")
    new BitmapCache(
      context,
      new URL(s"https://secure.gravatar.com/avatar/$md5sum?s=$size&d=$defaultEncoded").openConnection.asInstanceOf[HttpURLConnection],
      "gravatar",
      md5sum,
      3600 * 12).get()
  }
}
