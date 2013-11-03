package org.fedoraproject.mobile

import Badges.LeaderboardUser

import Implicits._
import util.Hashing

import android.app.Activity
import android.content.{ Context, Intent }
import android.net.Uri
import android.view.{ LayoutInflater, View, ViewGroup }
import android.view.View.OnClickListener
import android.widget.{ ArrayAdapter, ImageView, LinearLayout, TextView }

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

    layout
      .findViewById(R.id.rank)
      .asInstanceOf[TextView]
      .setText("#%02d".format(item.rank))

    val iconView = layout
      .findViewById(R.id.icon)
      .asInstanceOf[ImageView]

    // If there's a user associated with it, pull from gravatar.
    // Otherwise, just use the icon if it exists.
    val email = s"${item.nickname}@fedoraproject.org"
    Cache.getGravatar(
      context,
      Hashing.md5(email)).onComplete { result =>
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

    // Use type ascriptions to work around Int <-> Integer annoyances.
    // Blame Java for this. Or blame Scala if you want. Either way, it's
    // annoying.
    val badgesQuantity = context.getResources.getQuantityString(
      R.plurals.badges_lb_badges,
      item.badges: java.lang.Integer,
      item.badges: java.lang.Integer)

    layout
      .findViewById(R.id.badges)
      .asInstanceOf[TextView]
      .setText(badgesQuantity)

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
