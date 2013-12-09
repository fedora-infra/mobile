package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import com.google.common.io.CharStreams

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

object Badges {
  val url = "https://badges.fedoraproject.org/"

  case class Badge(
    id: String,
    name: String,
    image: String,
    firstAwarded: Option[Float],
    firstAwardedPerson: Option[String],
    lastAwarded: Option[Float],
    lastAwardedPerson: Option[String],
    percentEarned: Float,
    timesAwarded: Int,
    description: String,
    issued: Option[Float] = None) // Only in the 'user' JSON.

  case class User(
    percentEarned: Float,
    user: String,
    assertions: List[Badge])

  case class LeaderboardUser(
    nickname: String,
    badges: Int,
    rank: Int)

  case class Leaderboard(leaderboard: List[LeaderboardUser])

  implicit def BadgeCodecJson: CodecJson[Badge] =
    casecodec11(Badge.apply, Badge.unapply)("id", "name", "image", "first_awarded", "first_awarded_person", "last_awarded", "last_awarded_person", "percent_earned", "times_awarded", "description", "issued")

  implicit def UserCodecJson: CodecJson[User] =
    casecodec3(User.apply, User.unapply)("percent_earned", "user", "assertions")

  implicit def LeaderboardUserCodecJson: CodecJson[LeaderboardUser] =
    casecodec3(LeaderboardUser.apply, LeaderboardUser.unapply)("nickname", "badges", "rank")

  implicit def LeaderboardCodecJson: CodecJson[Leaderboard] =
    casecodec1(Leaderboard.apply, Leaderboard.unapply)("leaderboard")

  /** Returns JSON after completing the query. */
  def query(path: String): IO[String] = IO {
    Log.v("Badges", "Beginning query")
    val uri = Uri.parse(url + path.dropWhile(_ == '/'))
    val connection = new URL(uri.toString)
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
  }

  def info(id: String): Promise[String] = promise {
    query(s"/badge/${id}/json").unsafePerformIO
  }

  def user(user: String): Promise[String \/ User] = promise {
    query(s"/badge/${user}/json").unsafePerformIO.decodeEither[User]
  }

  def leaderboard(): Promise[String \/ List[LeaderboardUser]] = promise {
    query("/leaderboard/json")
      .unsafePerformIO
      .decodeEither[Leaderboard]
      .map(_.leaderboard)
  }
}
