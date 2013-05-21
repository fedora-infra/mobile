package org.fedoraproject.mobile

import Implicits._

import android.app.Activity
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.{ AdapterView, ArrayAdapter }

import spray.json._

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

case class StatusesResponse(global_info: String, services: Map[String, Map[String, String]])

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val f = jsonFormat2(StatusesResponse.apply)
}

import MyJsonProtocol._

class StatusActivity extends NavDrawerActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setUpNav(R.layout.statuses)

    future {
      Source.fromURL("http://status.fedoraproject.org/statuses.json").mkString
    }.onComplete { result =>
      result match {
        case Success(e) => {
          val parsed = JsonParser(e).convertTo[StatusesResponse]

          val entries = parsed.services.map { case (service, info) =>
            service + " - " + info("status")
          }

          val adapter = new ArrayAdapter[String](
            this,
            android.R.layout.simple_list_item_1,
            entries.toArray)

          runOnUiThread {
            findView(TR.statuses).setAdapter(adapter)
          }
        }
        case Failure(e) =>
      }
    }
  }
}
