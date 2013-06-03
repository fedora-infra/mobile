package org.fedoraproject.mobile

import android.content.Context
import android.graphics.{ Bitmap, BitmapFactory }
import android.util.Log

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.{ File, BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream, InputStream }
import java.net.{ URL, URLEncoder }

object Cache {
  /** Returns a [[Future[Bitmap]]] containing the icon for a package.
    *
    * This method will check to see if the image is cached. If it is, it will
    * not bother making an HTTP request for the image. If it is not, it will
    * get the image, save it to cache, then load it from cache.
    *
    * @param context The context on which to act.
    * @param path The name of the image on the server (and in cache).
    */
  def getPackageIcon(context: Context, path: String): Future[Bitmap] = {
    future {
      val cache = context.getCacheDir
      val iconsDir = new File(cache.toString + "/package_icons")
      iconsDir.mkdir
      val iconFile = new File(iconsDir.toString + "/" + path)
      if (iconFile.exists) {
        Log.v("PackageSearchActivity", "Icon already cached.")
      } else {
        Log.v("PackageSearchActivity", "Icon pulled from HTTP.")
        val url = new URL(s"https://apps.fedoraproject.org/packages/images/icons/${path}.png")
          .getContent
          .asInstanceOf[InputStream]
        val outputStream = new BufferedOutputStream(new FileOutputStream(iconFile))
        Iterator
          .continually(url.read)
          .takeWhile(-1 !=)
          .foreach(outputStream.write)
        outputStream.flush
      }
      val inputStream = new BufferedInputStream(new FileInputStream(iconFile))
      BitmapFactory.decodeStream(inputStream)
    }
  }

  /** Returns a [[Future[Bitmap]]] containing a Gravatar.
    *
    * Like getPackageIcon, this method will check to see if the image is cached.
    * If it is, it will not bother making an HTTP request for the image. If it
    * is not, it will get the image, save it to cache, then load it from cache.
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
    future {
      val cache = context.getCacheDir
      val dir = new File(cache.toString + "/gravatar")
      dir.mkdir
      val file = new File(dir.toString + "/" + md5sum + ".jpg")
      if (file.exists) {
        Log.v(context.getClass.getSimpleName, "Gravatar already cached.")
      } else {
        Log.v(context.getClass.getSimpleName, "Gravatar pulled from HTTP.")
        val defaultEncoded = URLEncoder.encode(default, "utf8")
        val url = new URL(
          s"https://secure.gravatar.com/avatar/$md5sum?s=$size&d=$defaultEncoded")
          .getContent
          .asInstanceOf[InputStream]
        val outputStream = new BufferedOutputStream(new FileOutputStream(file))
        Iterator
          .continually(url.read)
          .takeWhile(-1 !=)
          .foreach(outputStream.write)
        outputStream.flush
      }
      val inputStream = new BufferedInputStream(new FileInputStream(file))
      BitmapFactory.decodeStream(inputStream)
    }
  }
}
