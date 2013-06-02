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
}
