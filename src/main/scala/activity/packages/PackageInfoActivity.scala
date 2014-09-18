package org.fedoraproject.mobile

import Pkgwat._
import Implicits._
import util.Hashing

import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.{ TableRow, TextView, Toast }

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._

import scala.io.Source
import scala.language.reflectiveCalls

class PackageInfoActivity extends TypedActivity with util.Views {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setContentView(R.layout.package_info_activity)
    // TODO: Safe-cast
    val pkg = getIntent.getSerializableExtra("package").asInstanceOf[FedoraPackage]
    val actionbar = getActionBar
    val iconView = findView(TR.icon)

    BitmapFetch.fromPackage(pkg).runAsync(_.fold(
      err => {
        Log.e("PackageInfoActivity", err.toString)
        ()
      },
      icon => {
        runOnUiThread(actionbar.setIcon(new BitmapDrawable(getResources, icon)))
        ()
      }
    ))

    actionbar.setTitle(pkg.name)
    actionbar.setSubtitle(pkg.summary)

    findView(TR.description).setText(pkg.description.replaceAll("\n", " "))

    pkg.develOwner.cata(
      owner => {
        val ownerView = findView(TR.owner)
        ownerView.setText(owner)
        BitmapFetch.fromGravatarEmail(s"${owner}@fedoraproject.org").runAsync(_.fold(
          err => {
            Log.e("PackageInfoActivity", err.toString)
            ()
          },
          gravatar => {
            runOnUiThread(
              ownerView.setCompoundDrawablesWithIntrinsicBounds(
                new BitmapDrawable(getResources, gravatar), null, null, null))
            ()
          }
        ))
      },
      ()
    )

    // This should move to webapi/Pkgwat.scala
    val queryResult = queryActiveReleasesRaw(this, pkg)

    // TODO: runAsync effectively acts as unsafePerformIO here. Purify this.
    queryResult.runAsync(_.fold(
      _ => Toast.makeText(this, R.string.packages_release_failure, Toast.LENGTH_LONG).show,
      res => {
        findViewOpt(TR.progress).map(v => runOnUiThread(v.setVisibility(View.GONE)))

        // This is *really* hacky, but blocked on
        // https://github.com/fedora-infra/fedora-packages/issues/24.
        // The issue is that right now Fedora Packages (the app)'s API
        // returns strings of HTML in some of its responses. We have to
        // strip out the HTML in most cases, but in one case here, we
        // want to use the HTML to split on, so that we can nuke the karma
        // that we also get back in testing_version, since we only care
        // about the version number. Ideally we'd actually get an object
        // back in JSON, and we could split that into a Version object
        // locally, here in Scala-land. This object would have: version,
        // karma, and karma_icon. But for now, life isn't ideal.
        def stripHTML(s: String) = s.replaceAll("""<\/?.*?>""", "")

        val apiResults = stripHTML(res).decodeEither[Pkgwat.APIResults[FedoraRelease]]

        apiResults.fold(
          _ => Toast.makeText(this, R.string.packages_release_failure, Toast.LENGTH_LONG).show,
          r => {
            val releasesTable = findViewOpt(TR.releases)
            val header = new TableRow(this)

            // Okay, this is worthy of a sigh.
            val releaseTV = new TextView(this)
            releaseTV.setText(R.string.release)
            releaseTV.setTypeface(null, Typeface.BOLD)
            header.addView(releaseTV)

            val stableTV = new TextView(this)
            stableTV.setText(R.string.stable)
            stableTV.setTypeface(null, Typeface.BOLD)
            header.addView(stableTV)

            val testingTV = new TextView(this)
            testingTV.setText(R.string.testing)
            testingTV.setTypeface(null, Typeface.BOLD)
            header.addView(testingTV)

            runOnUiThread(releasesTable.foreach(_.addView(header)))

            r.rows.foreach { release =>
              val row = new TableRow(this)

              val rowReleaseTV = new TextView(this)
              rowReleaseTV.setText(stripHTML(release.release))
              row.addView(rowReleaseTV)

              val rowStableTV = new TextView(this)
              rowStableTV.setText(stripHTML(release.stableVersion))
              row.addView(rowStableTV)

              val rowTestingTV = new TextView(this)
              // TODO: unsafe call to head.
              rowTestingTV.setText(stripHTML(release.testingVersion.split("<div").head))
              row.addView(rowTestingTV)
              runOnUiThread(releasesTable.foreach(_.addView(row)))
            }
          }
        )
      }
    ))
    ()
  }
}
