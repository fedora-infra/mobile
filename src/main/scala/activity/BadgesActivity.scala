package org.fedoraproject.mobile

import Badges.JSONParsing._

import Implicits._

import android.os.Bundle

import spray.json._

// import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Success }

class BadgesActivity extends NavDrawerActivity {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.badges_activity)
  }
}
