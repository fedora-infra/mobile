package org.fedoraproject.mobile

import Implicits._
import util.Hashing

import android.app.Activity
import android.content.{ Context, Intent }
import android.graphics.Bitmap
import android.util.Log
import android.net.Uri
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.{ OnClickListener, OnLongClickListener }
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, ListView, TextView, Toast }

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Try, Success }

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
    val defaultIcon = item.icon.getOrElse("https://fedoraproject.org/static/images/fedora_infinity_64x64.png")
    val primaryUser = item.usernames.headOption
    val image: Future[Bitmap] = primaryUser match {
      case Some(username) => {
        Cache.getGravatar(
          context,
          Hashing.md5(s"${item.usernames.head}@fedoraproject.org").toString,
          default = defaultIcon)
      }
      case None => Cache.getServiceIcon(context, defaultIcon, item.title)
    }

    // XXX: Move this to scalaz Promise.
    image.onComplete {
      case Success(img) => activity.runOnUiThread(iconView.setImageBitmap(img))
      case _ => Log.e("FedmsgAdapter", "Unable to fetch icon.")
    }

    layout
      .findViewById(R.id.title)
      .asInstanceOf[TextView]
      .setText(item.title)

    layout
      .findViewById(R.id.subtitle)
      .asInstanceOf[TextView]
      .setText(item.subtitle)

    layout
      .findViewById(R.id.timestamp)
      .asInstanceOf[TextView]
      .setText(item.timestamp("ago"))

    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        item.link.map { link =>
          val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link))
          activity.startActivity(intent)
        }
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
