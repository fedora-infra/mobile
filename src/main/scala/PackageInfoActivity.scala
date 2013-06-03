package org.fedoraproject.mobile

import Implicits._

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Try, Success }

import com.google.common.hash.Hashing

class PackageInfoActivity extends NavDrawerActivity {
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

    findView(TR.summary).setText(pkg.summary)
    findView(TR.description).setText(pkg.description.replaceAll("\n", " "))

    pkg.develOwner match {
      case Some(owner) => {
        val ownerView = findView(TR.owner)
        ownerView.setText(owner)
        Cache.getGravatar(
          this,
          Hashing.md5.hashBytes(s"$owner@fedoraproject.org".getBytes("utf8")).toString).onComplete { result =>
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
  }
}
