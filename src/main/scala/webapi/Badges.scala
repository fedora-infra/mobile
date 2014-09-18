package org.fedoraproject.mobile

import android.net.Uri
import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
import scalaz.effect._

import java.io.{ DataOutputStream, InputStreamReader }
import java.net.HttpURLConnection

import scala.io.{ Codec, Source }

object Badges extends Webapi {
  val prodUrl = "https://badges.fedoraproject.org/"
  override val stagingUrl = Some("https://badges.stg.fedoraproject.org/")

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
  def query(context: Task[android.content.Context], path: String): Task[String] = {
    val connection = for {
      ctx <- context
      c <- connectionPath(ctx, path)
    } yield (c)

    def perform(c: HttpURLConnection): Task[String] = Task {
      c.setRequestMethod("GET")
      Source.fromInputStream(c.getInputStream)(Codec.UTF8).mkString
    }

    for {
      _ <- Pure.logV("Badges", "Beginning query")
      c <- connection
      res <- perform(c)
    } yield res
  }

  def info(id: String)(implicit context: Task[android.content.Context]): Task[String] =
    query(context, s"/badge/${id}/json")

  def user(user: String)(implicit context: Task[android.content.Context]): Task[String \/ User] =
    query(context, s"/badge/${user}/json") ∘ (_.decodeEither[User])

  def leaderboard(implicit context: Task[android.content.Context]): Task[String \/ List[LeaderboardUser]] =
    query(context, "/leaderboard/json") ∘ (_.decodeEither[Leaderboard]) ∘ (_.map(_.leaderboard))

}
