package org.fedoraproject.mobile

import Implicits._
import util.{ BitmapTransformations, Hashing }

import android.app.Activity
import android.content.{ Context, Intent }
import android.graphics.Bitmap
import android.util.Log
import android.net.Uri
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.{ OnClickListener, OnLongClickListener }
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, ListView, TextView, Toast }

import scalaz._, Scalaz._

import java.util.ArrayList // TODO: Do something about this.

class FedmsgAdapter(
  context: Context,
  resource: Int,
  items: ArrayList[HRF.Result])
  extends ArrayAdapter[HRF.Result](context, resource, items) {

  val activity = context.asInstanceOf[Activity]

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val item = getItem(position)

    val layout = LayoutInflater.from(context)
      .inflate(R.layout.fedmsg_list_item, parent, false)
      .asInstanceOf[LinearLayout]

    val iconView = layout
      .findViewById(R.id.icon)
      .asInstanceOf[ImageView]

    // If there is a username, use their libravatar/gravatar and fall back to
    // (a) the primary icon if it exists, or (b) the Fedora infinity logo, by
    // redirecting on the gravatar side.
    // If there is no username, follow the same a/b path as above, but on our
    // side without redirects. i.e., simply load the primary icon or the
    // infinity logo.
    val serviceIcon =
      item.icon.getOrElse("https://fedoraproject.org/static/images/fedora_infinity_64x64.png")
    val primaryUser = item.usernames.headOption
    val image = primaryUser.cata(
      username => BitmapFetch.fromGravatarEmail(s"${username}@fedoraproject.org"),
      BitmapFetch.fromURL(serviceIcon)
    )

    image.runAsync(_.fold(
      err => {
        Log.e("FedmsgAdapter", err.toString)
        ()
      },
      img => {
        val rounded = BitmapTransformations.roundCorners(img, 5)
        activity.runOnUiThread(iconView.setImageBitmap(rounded))
        ()
      }
    ))

    layout
      .findViewById(R.id.subtitle)
      .asInstanceOf[TextView]
      .setText(item.subtitle)

    val timestamp =
      if (item.date === "seconds ago")
        ((System.currentTimeMillis / 1000) - item.timestamp).toInt match {
          case 1 => "one second ago"
          case n => n.toString |+| " seconds ago"
        }
      else
        item.date


    layout
      .findViewById(R.id.timestamp)
      .asInstanceOf[TextView]
      .setText(timestamp)

    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        item.link.map { link =>
          val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link))
          activity.startActivity(intent)
        }
        ()
      }
    })

    layout.setOnLongClickListener(new OnLongClickListener() {
      override def onLongClick(view: View): Boolean = {
        val intent = new Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, item.subtitle)
        intent.setType("text/plain")
        activity.startActivity(intent)
        true
      }
    })

    layout
  }
}
