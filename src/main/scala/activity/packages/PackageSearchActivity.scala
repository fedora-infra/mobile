package org.fedoraproject.mobile

import Implicits._

import Pkgwat._

import android.app.SearchManager
import android.content.{ Context, Intent }
import android.os.Bundle
import android.view.{ LayoutInflater, Menu, View, ViewGroup }
import android.widget.AdapterView.OnItemClickListener
import android.widget.{ AdapterView, ArrayAdapter, ImageView, LinearLayout, ListView, TextView, Toast, SearchView }

import scalaz._, Scalaz._

// Needed until Cache moves to scalaz.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Try, Success }

class PackageSearchActivity extends NavDrawerActivity with util.Views {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setUpNav(R.layout.package_search_activity)
    handleIntent(getIntent())
  }

  override def onNewIntent(intent: Intent) {
    setIntent(intent)
    handleIntent(intent)
  }

  def handleIntent(intent: Intent) {
    if (intent.getAction == Intent.ACTION_SEARCH) {

      findViewOpt(TR.packages).map {
        _.tap { obj =>
          obj.setAdapter(null)
          obj.setVisibility(View.GONE)
        }
      }

      findViewOpt(TR.progress).map(_.setVisibility(View.VISIBLE))

      val queryText = intent.getStringExtra(SearchManager.QUERY)
      val queryObject = FilteredQuery(
        500, // TODO: Pagination...and, well, unhacking this.
        0,
        Map("search" -> queryText))

      Pkgwat.queryJson(queryObject) map {
        case \/-(res) => {
          val packages = res.rows.toArray

          class PackageAdapter(
            context: Context,
            resource: Int,
            items: Array[Package])
            extends ArrayAdapter[Package](context, resource, items) {
            override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
              val pkg = getItem(position)

              val layout = LayoutInflater.from(context)
                .inflate(R.layout.package_list_item, parent, false)
                .asInstanceOf[LinearLayout]

              val iconView = layout
                .findViewById(R.id.icon)
                .asInstanceOf[ImageView]

              Cache.getPackageIcon(PackageSearchActivity.this, pkg.icon) onComplete { result =>
                result match {
                  case Success(icon) => {
                    runOnUiThread {
                      iconView.setImageBitmap(icon)
                    }
                  }
                  case Failure(error) => {
                    runOnUiThread {
                      iconView.setImageResource(R.drawable.ic_search)
                    }
                  }
                }
              }

              layout
                .findViewById(R.id.title)
                .asInstanceOf[TextView]
                .setText(pkg.name)

              layout
            }
          }

          val adapter = new PackageAdapter(
            this,
            android.R.layout.simple_list_item_1,
            packages)

          runOnUiThread {
            findViewOpt(TR.progress).map(_.setVisibility(View.GONE))
          }

          runOnUiThread {
            val packagesView = findViewOpt(TR.packages).map(_.asInstanceOf[ListView])
            packagesView match {
              case Some(packagesView) => packagesView.tap { obj =>
                obj.setAdapter(adapter)
                obj.setVisibility(View.VISIBLE)
                obj.setOnItemClickListener(new OnItemClickListener {
                  def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
                    val pkg = packages(position)
                    val intent = new Intent(PackageSearchActivity.this, classOf[PackageInfoActivity])
                    intent.putExtra("package", pkg)
                    startActivity(intent)
                  }
                })
              }
              case None => // ...
            }
          }
        }
        case -\/(_) =>
          Toast.makeText(this, R.string.packages_search_failure, Toast.LENGTH_LONG).show
      }
    }
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.search, menu);

    val searchManager = getSystemService(Context.SEARCH_SERVICE).asInstanceOf[SearchManager]
    val searchView = menu.findItem(R.id.menu_search).getActionView.asInstanceOf[SearchView]
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName))
    searchView.setIconifiedByDefault(false)

    true
  }
}
