package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.OnClickListener
import android.widget.{ ArrayAdapter, LinearLayout, TextView }

class StatusAdapter(
  context: Context,
  resource: Int,
  items: Array[(String, Map[String, String])])
  extends ArrayAdapter[(String, Map[String, String])](context, resource, items) {
  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val entry = getItem(position)
    val service: Map[String, String] = entry._2

    val layout = LayoutInflater.from(context)
      .inflate(R.layout.status_list_item, parent, false)
      .asInstanceOf[LinearLayout]

    layout
      .setBackgroundResource(service("status") match {
        case "good" => R.drawable.status_good
        case "minor" | "scheduled" => R.drawable.status_minor
        case "major" => R.drawable.status_major
        case _ => R.drawable.status_unknown
      })

    layout
      .findViewById(R.id.servicename)
      .asInstanceOf[TextView]
      .setText(service("name"))

    val status = service("status") match {
      case "good" => R.string.status_good
      case "minor" => R.string.status_minor
      case "scheduled" => R.string.status_scheduled
      case "major" => R.string.status_major
      case _ => R.string.status_unknown
    }

    val statusView =
      layout
        .findViewById(R.id.servicestatus)
        .asInstanceOf[TextView]
      statusView.setText(status)
      StatusColor.colorFor(service("status")).map(statusView.setTextColor(_))

    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        val message =
          view
            .findViewById(R.id.servicemessage)
            .asInstanceOf[TextView]
          message.setText(service("message"))
          message.setVisibility(
            message.getVisibility match {
              case View.GONE => View.VISIBLE
              case View.VISIBLE => View.GONE
            })
      }
    })

    layout
  }
}

