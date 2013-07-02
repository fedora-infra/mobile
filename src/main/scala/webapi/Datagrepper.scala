package org.fedoraproject.mobile

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import android.net.Uri

/** This is a general purpose way to interact with the Datagrepper API.
  *
  * It provides a high level abstraction and hides all of the HTTP details
  * behind a nice Scala API. All HTTP methods within return a Future which,
  * in normal circumstances, will hold the result of the query.
  */
object Datagrepper {
  /** Returns a [[Future[List[Fedmsg.Message]]]] matching the query. */

  val url = "https://apps.fedoraproject.org/datagrepper/raw/"
  val uri = Uri.parse(url).buildUpon

  private def constructURL(arguments: List[(String, String)]): String = {
    arguments foreach { case (key, value) =>
      uri.appendQueryParameter(key, value)
    }
    uri.build.toString
  }

  def query(arguments: List[(String, String)]) = {
    future {
      Source.fromURL(constructURL(arguments)).mkString
    }
  }
}
