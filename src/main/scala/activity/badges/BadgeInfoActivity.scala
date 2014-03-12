package org.fedoraproject.mobile

import Badges.Badge

import Implicits._

import scalaz._, Scalaz._
import scalaz.effect.IO

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast

sealed case class BadgeSerializableCast[A](x: java.io.Serializable \/ A)
case object BadgeSerializableCast {
  def unsafeCastBadge(y: java.io.Serializable): BadgeSerializableCast[Badge] =
    if (y.isInstanceOf[Badge])
      BadgeSerializableCast(y.asInstanceOf[Badge].right)
    else
      BadgeSerializableCast(y.left)
}

class BadgeInfoActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle): Unit = {
    IO {
      super.onPostCreate(bundle)
      setContentView(R.layout.badge_info_activity)

      val badgeCast = BadgeSerializableCast.unsafeCastBadge(
        getIntent.getSerializableExtra("badge"))

      badgeCast.x match {
        case -\/(_) => Log.d("BadgeInfoActivity", "Unable to deserialize badge")
        case \/-(badge) => {
          val actionbar = getActionBar
          actionbar.setTitle(badge.name)

          val badgeImage = BitmapFetch.fromURL(badge.image)

          badgeImage runAsync {
            case -\/(err) => {
              Log.e("BadgeInfoActivity", "Unable to fetch badge image")
              ()
            }
            case \/-(image) => {
              runOnUiThread(findView(TR.icon).setImageBitmap(image))
              runOnUiThread(
                actionbar.setIcon(new BitmapDrawable(getResources, image)))
              ()
            }
          }

          findView(TR.name).setText(badge.name)
          findView(TR.description).setText(badge.description)
          val timeAgo: Option[String] =
            badge.issued.map(
              t =>
                DateUtils.getRelativeTimeSpanString(
                  this,
                  (t.toLong * 1000), true).toString)


          timeAgo.map { t =>
            val earned: String =
              getResources.getString(R.string.badges_earned_on).format(t)
            findView(TR.issued).setText(earned)
          }
        }
      }
    }.unsafePerformIO
    ()
  }
}
