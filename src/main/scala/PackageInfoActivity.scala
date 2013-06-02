package org.fedoraproject.mobile

import Implicits._

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.TextView

import scala.io.Source
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Try, Success }

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
  }
}
