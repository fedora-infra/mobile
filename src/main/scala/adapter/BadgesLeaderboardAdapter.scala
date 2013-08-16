package org.fedoraproject.mobile

import Badges.LeaderboardUser

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

class BadgesLeaderboardAdapter(
  context: Context,
  resource: Int,
  items: Array[LeaderboardUser])
  extends ArrayAdapter[LeaderboardUser](context, resource, items) {

  val activity = context.asInstanceOf[Activity]

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val item = getItem(position)

    val layout = LayoutInflater.from(context)
      .inflate(R.layout.badges_leaderboard_item, parent, false)
      .asInstanceOf[LinearLayout]

    val iconView = layout
      .findViewById(R.id.icon)
      .asInstanceOf[ImageView]

    // If there's a user associated with it, pull from gravatar.
    // Otherwise, just use the icon if it exists.
    val bytes = s"${item.nickname}@fedoraproject.org".getBytes("utf8")
    Cache.getGravatar(
      context,
      Hashing.md5.hashBytes(bytes).toString).onComplete { result =>
        result match {
          case Success(gravatar) => {
            activity.runOnUiThread {
              iconView.setImageBitmap(gravatar)
            }
          }
          case _ =>
        }
      }

    layout
      .findViewById(R.id.nickname)
      .asInstanceOf[TextView]
      .setText(item.nickname)

    layout
      .findViewById(R.id.rank)
      .asInstanceOf[TextView]
      .setText(context.getString(R.string.badges_lb_rank) + " " + item.rank)

    layout
      .findViewById(R.id.badges)
      .asInstanceOf[TextView]
      .setText(item.badges + " " + context.getString(R.string.badges_lb_badges))

    layout.setOnClickListener(new OnClickListener() {
      override def onClick(view: View): Unit = {
        val intent = new Intent(context, classOf[BadgesUserActivity])
        intent.putExtra("nickname", item.nickname)
        activity.startActivity(intent)
      }
    })

    layout
  }
}
