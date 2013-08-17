package org.fedoraproject.mobile

import Implicits._

import android.app.AlertDialog
import android.content.{ Context, DialogInterface }
import android.net.Uri
import android.util.Log

import com.google.common.io.CharStreams

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

object Updates {

  case class CommitStats(total: Int, additions: Int, deletions: Int)

  /** Parse the response from GitHub, only pulling out fields we care about. */
  case class Commit(sha: String, stats: CommitStats)

  object JSONParsing extends DefaultJsonProtocol {
    implicit val statsResponse = jsonFormat(CommitStats, "total", "additions", "deletions")
    implicit val commitResponse = jsonFormat(Commit, "sha", "stats")
  }

  import JSONParsing._

  /** Handle checking for updates when running in development mode.
    *
    * To do this successfully, we hit the GitHub API, and ask it for the shasum
    * of the latest commit. We compare it against the commit that we were built
    * from (which is android:versionName, modified by Fedora's Jenkins
    * instance). If they match, we do nothing. If they don't match, we pop up a
    * dialog, asking if the user wants to update.
    */
  private def getLatestCommit(): Future[Commit] = {
    Log.v("Updates", "Pinging GitHub API for latest commit info")
    val uri = Uri.parse("https://api.github.com/repos/fedora-infra/mobile/commits/HEAD")
    future {
      val connection = new URL(uri.toString)
        .openConnection
        .asInstanceOf[HttpURLConnection]
      connection setRequestMethod "GET"
      CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
    } map { res => JsonParser(res).convertTo[Commit] }
  }

  def compareVersion(context: Context): Future[Boolean] = {
    val version = context.getPackageManager.getPackageInfo(context.getPackageName, 0).versionName
    val c: Future[Commit] = getLatestCommit()
    c map { commit => commit.sha == version }
  }

  def presentDialog(context: Context): Unit = {
    val builder = new AlertDialog.Builder(context)

    builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
      }
    });

    builder.setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
      }
    });

    builder.setTitle(R.string.update_available)
    builder.setMessage(R.string.update_available_desc)
    val dialog = builder.create
    dialog.show
  }
}
