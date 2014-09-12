package org.fedoraproject.mobile

import Implicits._

import android.content.Intent
import android.net.Uri
import android.os.{ Bundle, Environment }
import android.util.Log
import android.widget.Toast

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ File, BufferedOutputStream, FileOutputStream }
import java.net.{ HttpURLConnection, URL }

class DownloadHeadActivity extends TypedActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.download_head_activity)

    // Run it.
    // I suspect we can purify some of this, too.
    main.runAsync(_.fold(
      err => {
        runOnUiThread(Toast.makeText(this, R.string.update_failure, Toast.LENGTH_LONG).show)
        Log.e("DownloadHeadActivity", err.toString)
        ()
      },
      apk => {
        val intent = new Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive")
        startActivity(intent)
      }
    ))
  }

  def downloadDir: Task[File] = Task {
    if (Environment.getExternalStorageState == Environment.MEDIA_MOUNTED) {
      new File(Environment.getExternalStorageDirectory + "/download/")
    } else {
      new File(getFilesDir, "/download/")
    }
  }

  def apkFile(path: File): Task[File] = Task {
    new File(path, "fedora-mobile-0.1.apk")
  }

  def deleteFile(file: File): Task[Unit] = Task {
    if (file.exists) {
      file.delete
      ()
    } else {
      ()
    }
  }

  def getNewestApk(file: File) = Task {
    val connection = new URL(
      "http://da.gd/fmsnap")
      .openConnection
      .asInstanceOf[HttpURLConnection]
    val urlStream = connection.getInputStream
    val outputStream = new BufferedOutputStream(new FileOutputStream(file))
    Iterator
      .continually(urlStream.read)
      .takeWhile(_ != -1)
      .foreach(outputStream.write)
    outputStream.flush
  }

  def main: Task[File] = for {
    _ <- Pure.logV("DownloadHeadActivity", "Starting download")
    path <- downloadDir
    _ <- Task(path.mkdirs)
    apk <- apkFile(path)
    _ <- deleteFile(apk)
    _ <- getNewestApk(apk)
  } yield apk
}
