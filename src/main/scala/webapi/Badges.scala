package org.fedoraproject.mobile

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
    issued: Option[Float] = None // Only in the 'user' JSON.
    )

  case class User(
    percentEarned: Float,
    user: String,
    assertions: List[Badge])

  case class LeaderboardUser(
    nickname: String,
    badges: Int,
    rank: Int)

  case class Leaderboard(leaderboard: List[LeaderboardUser])

  object JSONParsing extends DefaultJsonProtocol {
    implicit val badgeResponse = jsonFormat(Badge, "id", "name", "image", "first_awarded", "first_awarded_person", "last_awarded", "last_awarded_person", "percent_earned", "times_awarded", "description", "issued")
    implicit val userResponse = jsonFormat(User, "percent_earned", "user", "assertions")
    implicit val leaderboardUserResponse = jsonFormat(LeaderboardUser, "nickname", "badges", "rank")
    implicit val leaderboardResponse = jsonFormat(Leaderboard, "leaderboard")
  }

  /** Returns a [[Future[String]]] of JSON after completing the query. */
  def query(path: String) = {
    Log.v("Badges", "Beginning query")
    val uri = Uri.parse(url + path.dropWhile(_ == '/'))
    future {
      val connection = new URL(uri.toString)
        .openConnection
        .asInstanceOf[HttpURLConnection]
      connection setRequestMethod "GET"
      CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
    }
  }

  def forUser(user: String): Future[String] = query(s"/user/${user}/json")
  def info(id: String): Future[String] = query(s"/badge/${id}/json")
}
