package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.io.{ Codec, Source }

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
  def query(path: String): Task[String] = Task {
    Log.v("Badges", "Beginning query")
    val uri = Uri.parse(url + path.dropWhile(_ == '/'))
    val connection = new URL(uri.toString)
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def info(id: String): Task[String] =
    query(s"/badge/${id}/json")

  def user(user: String): Task[String \/ User] =
    query(s"/badge/${user}/json") ∘ (_.decodeEither[User])

  def leaderboard: Task[String \/ List[LeaderboardUser]] =
    query("/leaderboard/json") ∘ (_.decodeEither[Leaderboard]) ∘ (_.map(_.leaderboard))

}
