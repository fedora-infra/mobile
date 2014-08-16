package org.fedoraproject.mobile

import Implicits._

import android.app.AlertDialog
import android.content.{ Context, DialogInterface, Intent }
import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import scala.io.{ Codec, Source }

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

sealed trait JenkinsBuildStatus
case object Success extends JenkinsBuildStatus
case object Failure extends JenkinsBuildStatus

object Updates {
  case class CommitStats(total: Int, additions: Int, deletions: Int)

  /** Parse the response from GitHub, only pulling out fields we care about. */
  case class Commit(sha: String, stats: CommitStats)

  implicit def CommitStatsCodecJson: CodecJson[CommitStats] =
    casecodec3(CommitStats.apply, CommitStats.unapply)("total", "additions", "deletions")


  implicit def CommitCodecJson: CodecJson[Commit] =
    casecodec2(Commit.apply, Commit.unapply)("sha", "stats")

  implicit def JenkinsBuildStatusJson: DecodeJson[JenkinsBuildStatus] =
    DecodeJson(c => for {
      result <- (c --\ "result").as[String]
    } yield (if (result == "SUCCESS") Success else Failure))

  /** Get the latest build status from the Fedora Jenkins server. */
  def getJenkinsLastBuildStatus: Task[String \/ JenkinsBuildStatus] = delay {
    val connection =
      new URL("http://jenkins.cloud.fedoraproject.org/job/fedora-mobile/lastBuild/api/json")
      .openConnection
      .asInstanceOf[HttpURLConnection] // Note to self: Everything sucks.
    connection.setRequestMethod("GET")
    val str =
      Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
    str.decodeEither[JenkinsBuildStatus]
  }

  /** Handle checking for updates when running in development mode.
    *
    * To do this successfully, we hit the GitHub API, and ask it for the shasum
    * of the latest commit. We compare it against the commit that we were built
    * from (which is android:versionName, modified by Fedora's Jenkins
    * instance). If they match, we do nothing. If they don't match, we pop up a
    * dialog, asking if the user wants to update.
    */
  private def getLatestCommit: Task[String] = delay {
    Log.v("Updates", "Pinging GitHub API for latest commit info")
    val uri = Uri.parse("https://api.github.com/repos/fedora-infra/mobile/commits/HEAD")
    val connection = new URL(uri.toString)
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def compareVersion(context: Context): Task[String \/ Boolean] = {
    val version = context.getString(R.string.git_sha)
    val current = getLatestCommit
    // This can be cleaned up - Task is monadic.
    delay(current.run.decodeEither[Commit].map(_.sha == version))
  }

  def presentDialog(context: Context): Unit = {
    val builder = new AlertDialog.Builder(context)

    builder.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
        val intent = new Intent(context, classOf[DownloadHeadActivity])
        context.startActivity(intent)
      }
    })

    builder.setNegativeButton(R.string.not_now, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
      }
    })

    builder.setTitle(R.string.update_available)
    builder.setMessage(R.string.update_available_desc)
    val dialog = builder.create
    dialog.show
  }
}
