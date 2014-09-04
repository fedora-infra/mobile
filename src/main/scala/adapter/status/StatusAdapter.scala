package org.fedoraproject.mobile

import Implicits._

import android.content.Context
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.OnClickListener
import android.widget.{ ArrayAdapter, LinearLayout, TextView }

class StatusAdapter(
  context: Context,
  resource: Int,
  items: Array[(String, Status.StatusMetadata)])
  extends ArrayAdapter[(String, Status.StatusMetadata)](context, resource, items) {
  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val entry = getItem(position)
    val service: Status.StatusMetadata = entry._2

    val layout = LayoutInflater.from(context)
      .inflate(R.layout.status_list_item, parent, false)
      .asInstanceOf[LinearLayout]

    layout
      .setBackgroundResource(service.status match {
        case Some(Status.Good) => R.drawable.status_good
        case Some(Status.Minor) | Some(Status.Scheduled) => R.drawable.status_minor
        case Some(Status.Major) => R.drawable.status_major
        case _ => R.drawable.status_unknown
    })

    layout
      .findViewById(R.id.servicename)
      .asInstanceOf[TextView]
      .setText(service.name)

    val status = service.status match {
      case Some(Status.Good)      => R.string.status_good
      case Some(Status.Minor)     => R.string.status_minor
      case Some(Status.Scheduled) => R.string.status_scheduled
      case Some(Status.Major)     => R.string.status_major
      case _                      => R.string.status_unknown
    }

    val statusView =
      layout
        .findViewById(R.id.servicestatus)
        .asInstanceOf[TextView]
      statusView.setText(status)
      service.status.map(s => statusView.setTextColor(Status.colorFor(s)))


    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        val message =
          view
            .findViewById(R.id.servicemessage)
            .asInstanceOf[TextView]
          message.setText(service.message)
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

