package org.fedoraproject.mobile

import Badges.Badge
import Badges.JSONParsing._

import Implicits._

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgeInfoActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badge_info_activity)

    val badge = getIntent.getSerializableExtra("badge").asInstanceOf[Badge]

    val actionbar = getActionBar
    actionbar.setTitle(badge.name)

    val badgeImageFuture = Cache.getBadgeImage(
      this,
      badge.image,
      badge.id)

    badgeImageFuture onComplete { result =>
      result match {
        case Success(badge) => {
          runOnUiThread(findView(TR.icon).setImageBitmap(badge))
          runOnUiThread(
            actionbar.setIcon(new BitmapDrawable(getResources, badge)))
        }
        case _ =>
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
      val earned: String = getResources.getString(R.string.earned_on).format(t)
      findView(TR.issued).setText(earned)
    }
  }
}
