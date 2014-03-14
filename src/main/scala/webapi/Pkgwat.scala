package org.fedoraproject.mobile

import android.util.Log

import argonaut._, Argonaut._

import com.google.common.io.CharStreams

import scalaz._, Scalaz._
import scalaz.concurrent.Promise
import scalaz.concurrent.Promise._
import scalaz.effect._

import java.io.{ InputStreamReader }
import java.net.{ HttpURLConnection, URL, URLEncoder }

import scala.io.{ Codec, Source }

object Pkgwat {
  sealed trait ResultType

  case class APIResults[ResultType](
    visibleRows: Int,
    totalRows: Int,
    rowsPerPage: Int,
    startRow: Int,
    rows: List[ResultType])

  case class SubPackage(
    icon: String,
    description: String,
    link: String,
    summary: String,
    name: String,
    upstreamURL: Option[String] = None,
    develOwner: Option[String] = None)

  case class Package(
    icon: String,
    description: String,
    link: String,
    subPackages: List[SubPackage],
    summary: String,
    name: String,
    upstreamURL: Option[String] = None,
    develOwner: Option[String] = None) extends ResultType

  case class Release(
    release: String,
    stableVersion: String,
    testingVersion: String) extends ResultType

  case class FilteredQuery(rowsPerPage: Int, startRow: Int, filters: Map[String, String])

  implicit def FilteredQueryCodecJson: CodecJson[FilteredQuery] =
    casecodec3(FilteredQuery.apply, FilteredQuery.unapply)("rows_per_page", "start_row", "filters")

  implicit def ReleaseCodecJson: CodecJson[Release] =
    casecodec3(Release.apply, Release.unapply)("release", "stable_version", "testing_version")

  implicit def PackageCodecJson: CodecJson[Package] =
    casecodec8(Package.apply, Package.unapply)("icon", "description", "link", "sub_pkgs", "summary", "name", "upstream_url", "devel_owner")

  implicit def SubPackageCodecJson: CodecJson[SubPackage] =
    casecodec7(SubPackage.apply, SubPackage.unapply)("icon", "description", "link", "summary", "name", "upstream_url", "devel_owner")

  implicit def PackageResultCodecJson: CodecJson[APIResults[Package]] =
    casecodec5(APIResults.apply[Package], APIResults.unapply[Package])("visible_rows", "total_rows", "rows_per_page", "start_row", "rows")

  implicit def ReleaseResultCodecJson: CodecJson[APIResults[Release]] =
    casecodec5(APIResults.apply[Release], APIResults.unapply[Release])("visible_rows", "total_rows", "rows_per_page", "start_row", "rows")

  // TODO: Move this... It's public because it's used by PackageInfoActivity
  def constructURL(path: String, query: FilteredQuery): String = {
    val json: String = query.asJson.nospaces
    Seq(
      "https://apps.fedoraproject.org/packages/",
      "fcomm_connector",
      path,
      URLEncoder.encode(json, "utf8")).mkString("/")
  }

  private def query(query: FilteredQuery): IO[String] = IO {
    Log.v("Pkgwat", "Beginning query")
    val connection = new URL(constructURL("xapian/query/search_packages", query))
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def queryJson(q: FilteredQuery): Promise[String \/ APIResults[Package]] = promise {
    query(q)
    .unsafePerformIO
    .replaceAll("""<\/?.*?>""", "")
    .decodeEither[APIResults[Package]]
  }
}
