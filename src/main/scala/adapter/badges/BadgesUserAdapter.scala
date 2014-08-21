package org.fedoraproject.mobile

import Badges.Badge

import Implicits._

import scalaz._, Scalaz._

import android.app.Activity
import android.content.{ Context, Intent }
import android.net.Uri
import android.util.Log
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.OnClickListener
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, TextView }

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

    BitmapFetch.fromURL(item.image).runAsync(_.fold(
      err => {
        Log.e("BadgesUserAdapter", err.toString)
        ()
      },
      badge => {
        activity.runOnUiThread(iconView.setImageBitmap(badge))
        ()
      }
    ))

    layout
      .findViewById(R.id.name)
      .asInstanceOf[TextView]
      .setText(item.name)

    layout
      .findViewById(R.id.description)
      .asInstanceOf[TextView]
      .setText(item.description)

    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        val intent = new Intent(context, classOf[BadgeInfoActivity])
        intent.putExtra("badge", item)
        activity.startActivity(intent)
      }
    })

    layout
  }
}
