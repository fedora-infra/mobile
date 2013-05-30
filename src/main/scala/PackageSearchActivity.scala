package org.fedoraproject.mobile

import Implicits._

import android.app.{ Activity, SearchManager }
import android.content.{ Context, Intent }
import android.graphics.{ Bitmap, BitmapFactory }
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.{ LayoutInflater, Menu, View, ViewGroup }
import android.widget.AdapterView.OnItemClickListener
import android.widget.{ AdapterView, ArrayAdapter, ImageView, LinearLayout, ListView, TextView, Toast, SearchView }

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.{ Failure, Try, Success }

import java.io.{ File, BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream, InputStream }
import java.net.{ URL, URLEncoder }

case class APIResults[T](
  visibleRows: Int,
  totalRows: Int,
  rowsPerPage: Int,
  startRow: Int,
  rows: List[T])

case class Package(
  icon: String,
  description: String,
  link: String,
  subPackages: Option[List[Package]],
  summary: String,
  name: String,
  upstreamURL: Option[String] = None,
  develOwner: Option[String] = None)

case class FilteredQuery(rowsPerPage: Int, startRow: Int, filters: Map[String, String])

object PackagesJsonProtocol extends DefaultJsonProtocol {
  implicit val filteredQueryFormat = jsonFormat(FilteredQuery, "rows_per_page", "start_row", "filters")
  implicit val packageFormat: JsonFormat[Package] = lazyFormat(jsonFormat(Package, "icon", "description", "link", "sub_pkgs", "summary", "name", "upstream_url", "devel_owner"))
  implicit val packageResultFormat = jsonFormat(APIResults[Package], "visible_rows", "total_rows", "rows_per_page", "start_row", "rows")
}

import PackagesJsonProtocol._

class PackageSearchActivity extends NavDrawerActivity {
  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setUpNav(R.layout.package_search)
    handleIntent(getIntent())
  }

  override def onNewIntent(intent: Intent) {
    setIntent(intent)
    handleIntent(intent)
  }

  private def constructURL(path: String, query: FilteredQuery): String = {
    val json = query.toJson.compactPrint
    Seq(
      "https://apps.fedoraproject.org/packages/",
      "fcomm_connector",
      path,
      URLEncoder.encode(json, "utf8")).mkString("/")
  }

  /** Returns a [[Future[Bitmap]]] containing the icon for a package.
    *
    * This method will check to see if the image is cached. If it is, it will
    * not bother making an HTTP request for the image. If it is not, it will
    * get the image, save it to cache, then load it from cache.
    *
    * @param path The name of the image on the server (and in cache).
    */
  private def getCachedIcon(path: String): Future[Bitmap] = {
    future {
      val cache = getCacheDir
      val iconsDir = new File(cache.toString + "/package_icons")
      iconsDir.mkdir
      val iconFile = new File(iconsDir.toString + "/" + path)
      val inputStream = if (iconFile.exists) {
        Log.v("PackageSearchActivity", "Icon pulled from cache.")
        new BufferedInputStream(new FileInputStream(iconFile))
      } else {
        Log.v("PackageSearchActivity", "Icon pulled from HTTP.")
        val url = new URL(s"https://apps.fedoraproject.org/packages/images/icons/${path}.png")
          .getContent
          .asInstanceOf[InputStream]
        val outputStream = new BufferedOutputStream(new FileOutputStream(iconFile))
        Iterator
          .continually(url.read)
          .takeWhile(-1 !=)
          .foreach(outputStream.write)
        outputStream.flush
        url
      }
      BitmapFactory.decodeStream(inputStream)
    }
  }

  def handleIntent(intent: Intent) {
    if (intent.getAction == Intent.ACTION_SEARCH) {

      Option(findView(TR.packages)) match {
        case Some(packagesListView) => packagesListView.asInstanceOf[ListView].setAdapter(null)
        case None =>
      }

      val query = intent.getStringExtra(SearchManager.QUERY)
      val jsonURL = constructURL(
        "xapian/query/search_packages",
        FilteredQuery(
          500, // TODO: Pagination...and, well, unhacking this.
          0,
          Map("search" -> query)))

      future {
        Source.fromURL(jsonURL).mkString
      } onComplete { result =>
        result match {
          case Success(content) => {
            val result = JsonParser(content.replaceAll("""<\/?.*?>""", "")).convertTo[APIResults[Package]]
            val packages = result.rows.toArray

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

                getCachedIcon(pkg.icon) onComplete { result =>
                  result match {
                    case Success(icon) => {
                      runOnUiThread {
                        layout
                          .findViewById(R.id.icon)
                          .asInstanceOf[ImageView]
                          .setImageBitmap(icon)
                      }
                      notifyDataSetChanged
                    }
                    case Failure(error) => {
                      runOnUiThread {
                        layout
                          .findViewById(R.id.icon)
                          .asInstanceOf[ImageView]
                          .setImageResource(R.drawable.ic_search)
                      }
                      notifyDataSetChanged
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
              val packagesView = Option(findView(TR.packages))
              packagesView match {
                case Some(packagesView) => packagesView.asInstanceOf[ListView].setAdapter(adapter)
                case None => // ...
              }
            }
          }
          case Failure(e) => Toast.makeText(this, R.string.packages_search_failure, Toast.LENGTH_LONG).show
        }
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
