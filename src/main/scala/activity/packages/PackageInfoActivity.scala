package org.fedoraproject.mobile

import Implicits._
import util.Hashing

import Pkgwat._

import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.{ TableRow, TextView, Toast }

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._

import scala.concurrent.future // TODO: Nuke
import scala.concurrent.ExecutionContext.Implicits.global // TODO: Nuke
import scala.io.Source
import scala.util.{ Failure, Try, Success }

class PackageInfoActivity extends NavDrawerActivity with util.Views {
  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    setUpNav(R.layout.package_info_activity)
    val pkg = getIntent.getSerializableExtra("package").asInstanceOf[Package]

    val actionbar = getActionBar

    lazy val iconView = findView(TR.icon)

    Cache.getPackageIcon(this, pkg.icon) onComplete { result =>
      result match {
        case Success(icon) => {
          runOnUiThread {
            actionbar.setIcon(new BitmapDrawable(getResources, icon))
          }
        }
        case Failure(error) => {
          runOnUiThread {
            iconView.setImageResource(R.drawable.ic_search)
          }
        }
      }
    }

    actionbar.setTitle(pkg.name)
    actionbar.setSubtitle(pkg.summary)

    findView(TR.description).setText(pkg.description.replaceAll("\n", " "))

    pkg.develOwner match {
      case Some(owner) => {
        val ownerView = findView(TR.owner)
        ownerView.setText(owner)
        Cache.getGravatar(
          this,
          Hashing.md5(s"$owner@fedoraproject.org").toString).onComplete { result =>
            result match {
              case Success(gravatar) => {
                runOnUiThread {
                  ownerView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources, gravatar), null, null, null)
                }
              }
              case _ =>
            }
          }
      }
      case None =>
    }

    val jsonURL = constructURL(
      "bodhi/query/query_active_releases",
      FilteredQuery(
        20,
        0,
        Map("package" -> pkg.name)))

    promise {
      Source.fromURL(jsonURL).mkString
    } map {
      case res: String => {
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

          val apiResults = stripHTML(res).decodeEither[Pkgwat.APIResults[Release]]

          apiResults match {
            case \/-(r) => {
              val releasesTable = findViewOpt(TR.releases)
              val header = new TableRow(this)
              header.addView(
                new TextView(this).tap { obj =>
                  obj.setText(R.string.release)
                  obj.setTypeface(null, Typeface.BOLD)
              })
              header.addView(
                new TextView(this).tap { obj =>
                  obj.setText(R.string.stable)
                  obj.setTypeface(null, Typeface.BOLD)
              })

              header.addView(
                new TextView(this).tap { obj =>
                  obj.setText(R.string.testing)
                  obj.setTypeface(null, Typeface.BOLD)
              })

              runOnUiThread(releasesTable.foreach(_.addView(header)))

              r.rows.foreach { release =>
                val row = new TableRow(this)
                row.addView(new TextView(this).tap(_.setText(stripHTML(release.release))))
                row.addView(new TextView(this).tap(_.setText(stripHTML(release.stableVersion))))
                row.addView(new TextView(this).tap(_.setText(stripHTML(release.testingVersion.split("<div").head)))) // HACK
                runOnUiThread(releasesTable.foreach(_.addView(row)))
              }
            }
            case -\/(r) =>
              Toast.makeText(this, R.string.packages_release_failure, Toast.LENGTH_LONG).show
          }
      }
      case _ => // TODO: compose this with the above, remove duplicate code
        Toast.makeText(this, R.string.packages_release_failure, Toast.LENGTH_LONG).show
    }
  }
}
