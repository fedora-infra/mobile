package org.fedoraproject.mobile

import android.content.Context
import android.graphics.{ Bitmap, BitmapFactory }
import android.util.Log

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.{ File, BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream, InputStream }
import java.net.{ URL, URLEncoder }

object Cache {
  /** Returns a [[Future[Bitmap]]] containing an image, given parameters.
    *
    * This method provides general purpose access to the cache, given a set of
    * parameters that remain consistent between accesses.
    *
    * The cache is "general purpose" in that you give it a URL and some other
    * parameters, and it will cache the image how you tell it to, rather than
    * needing to duplicate logic everywhere.
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
  private def cacheImage(context: Context, url: URL, directoryName: String, fileName: String) =
    future {
      val cache = context.getCacheDir
      val directory = new File(cache.toString + "/" + directoryName)
      directory.mkdir
      val file = new File(directory.toString + "/" + fileName)
      if (file.exists) {
        Log.v("Cache.cacheImage", s"$directoryName/$fileName already cached.")
      } else {
        Log.v("Cache.cacheImage", s"$url pulled from HTTP.")
        val urlStream = url.getContent.asInstanceOf[InputStream]
        val outputStream = new BufferedOutputStream(new FileOutputStream(file))
        Iterator
          .continually(urlStream.read)
          .takeWhile(-1 !=)
          .foreach(outputStream.write)
        outputStream.flush
      }
      val inputStream = new BufferedInputStream(new FileInputStream(file))
      BitmapFactory.decodeStream(inputStream)
    }

  /** Returns a [[Future[Bitmap]]] containing the icon for a package.
    *
    * @param context The context on which to act.
    * @param path The name of the image on the server (and in cache).
    */
  def getPackageIcon(context: Context, path: String): Future[Bitmap] =
    cacheImage(
      context,
      new URL(s"https://apps.fedoraproject.org/packages/images/icons/${path}.png"),
      "package_icons",
      path)

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
    cacheImage(
      context,
      new URL(s"https://secure.gravatar.com/avatar/$md5sum?s=$size&d=$defaultEncoded"),
      "gravatar",
      md5sum)
  }
}
