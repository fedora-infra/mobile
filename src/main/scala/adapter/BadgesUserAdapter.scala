package org.fedoraproject.mobile

import Badges.Badge

import Implicits._

import android.app.Activity
import android.content.{ Context, Intent }
import android.net.Uri
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.OnClickListener
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, TextView }

import com.google.common.hash.Hashing

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Try, Success }

class BadgesUserAdapter(
  context: Context,
  resource: Int,
  items: Array[Badge])
  extends ArrayAdapter[Badge](context, resource, items) {

  val activity = context.asInstanceOf[Activity]

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val item = getItem(position)

    val layout = LayoutInflater.from(context)
      .inflate(R.layout.badges_user_item, parent, false)
      .asInstanceOf[LinearLayout]

    val iconView = layout
      .findViewById(R.id.icon)
      .asInstanceOf[ImageView]

    val badgeImageFuture = Cache.getBadgeImage(
      context,
      s"http://infrastructure.fedoraproject.org/infra/badges/pngs/${item.image}",
      item.name)

    badgeImageFuture onComplete { result =>
      result match {
        case Success(badge) => {
          activity.runOnUiThread {
            iconView.setImageBitmap(badge)
          }
        }
        case _ =>
      }
    }

    layout
      .findViewById(R.id.name)
      .asInstanceOf[TextView]
      .setText(item.name)

    layout
      .findViewById(R.id.description)
      .asInstanceOf[TextView]
      .setText(item.description)

    /*layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        item.link.map { link =>
          val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link))
          activity.startActivity(intent)
        }
      }
    })*/

    layout
  }
}
