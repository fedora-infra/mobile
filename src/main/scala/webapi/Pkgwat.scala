package org.fedoraproject.mobile

import android.util.Log

import com.google.common.io.CharStreams

import spray.json._

import scala.concurrent.{ future, Future }
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.{ InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

object Pkgwat {
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

  case class Release(
    release: String,
    stableVersion: String,
    testingVersion: String)

  case class FilteredQuery(rowsPerPage: Int, startRow: Int, filters: Map[String, String])

  object JSONParsing extends DefaultJsonProtocol {
    implicit val filteredQueryFormat = jsonFormat(FilteredQuery, "rows_per_page", "start_row", "filters")

    implicit val packageFormat: JsonFormat[Package] = lazyFormat(jsonFormat(Package, "icon", "description", "link", "sub_pkgs", "summary", "name", "upstream_url", "devel_owner"))
    implicit val packageResultFormat = jsonFormat(APIResults[Package], "visible_rows", "total_rows", "rows_per_page", "start_row", "rows")

    implicit val releaseFormat = jsonFormat(Release, "release", "stable_version", "testing_version")
    implicit val releaseResultFormat = jsonFormat(APIResults[Release], "visible_rows", "total_rows", "rows_per_page", "start_row", "rows")
  }

  import JSONParsing._

  def constructURL(path: String, query: FilteredQuery): String = {
    val json = query.toJson.compactPrint
    Seq(
      "https://apps.fedoraproject.org/packages/",
      "fcomm_connector",
      path,
      URLEncoder.encode(json, "utf8")).mkString("/")
  }

  def query(query: FilteredQuery) = {
    Log.v("Pkgwat", "Beginning query")
    future {
      val connection = new URL(constructURL("xapian/query/search_packages", query))
        .openConnection
        .asInstanceOf[HttpURLConnection]
      connection setRequestMethod "GET"
      CharStreams.toString(new InputStreamReader(connection.getInputStream, "utf8"))
    }
  }
}
