package org.fedoraproject.mobile

import android.util.Log

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.concurrent.Task._
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

  case class FedoraSubPackage(
    icon: String,
    description: String,
    link: String,
    summary: String,
    name: String,
    upstreamURL: Option[String] = None,
    develOwner: Option[String] = None)

  case class FedoraPackage(
    icon: String,
    description: String,
    link: String,
    subPackages: List[FedoraSubPackage],
    summary: String,
    name: String,
    upstreamURL: Option[String] = None,
    develOwner: Option[String] = None) extends ResultType

  case class FedoraRelease(
    release: String,
    stableVersion: String,
    testingVersion: String) extends ResultType

  case class FilteredQuery(rowsPerPage: Int, startRow: Int, filters: Map[String, String])

  implicit def FilteredQueryCodecJson: CodecJson[FilteredQuery] =
    casecodec3(FilteredQuery.apply, FilteredQuery.unapply)("rows_per_page", "start_row", "filters")

  implicit def ReleaseCodecJson: CodecJson[FedoraRelease] =
    casecodec3(FedoraRelease.apply, FedoraRelease.unapply)("release", "stable_version", "testing_version")

  implicit def FedoraPackageCodecJson: CodecJson[FedoraPackage] =
    casecodec8(FedoraPackage.apply, FedoraPackage.unapply)("icon", "description", "link", "sub_pkgs", "summary", "name", "upstream_url", "devel_owner")

  implicit def FedoraSubPackageCodecJson: CodecJson[FedoraSubPackage] =
    casecodec7(FedoraSubPackage.apply, FedoraSubPackage.unapply)("icon", "description", "link", "summary", "name", "upstream_url", "devel_owner")

  implicit def APIResultsCodecJson: CodecJson[APIResults[FedoraPackage]] =
    casecodec5(APIResults.apply[FedoraPackage], APIResults.unapply[FedoraPackage])("visible_rows", "total_rows", "rows_per_page", "start_row", "rows")

  implicit def FedoraReleaseResultCodecJson: CodecJson[APIResults[FedoraRelease]] =
    casecodec5(APIResults.apply[FedoraRelease], APIResults.unapply[FedoraRelease])("visible_rows", "total_rows", "rows_per_page", "start_row", "rows")

  // TODO: Move this... It's public because it's used by PackageInfoActivity
  def constructURL(path: String, query: FilteredQuery): String = {
    val json: String = query.asJson.nospaces
    Seq(
      "https://apps.fedoraproject.org/packages/",
      "fcomm_connector",
      path,
      URLEncoder.encode(json, "utf8")).mkString("/")
  }

  private def query(query: FilteredQuery): Task[String] = Task {
    Log.v("Pkgwat", "Beginning query")
    val connection = new URL(constructURL("xapian/query/search_packages", query))
      .openConnection
      .asInstanceOf[HttpURLConnection]
    connection setRequestMethod "GET"
    Source.fromInputStream(connection.getInputStream)(Codec.UTF8).mkString
  }

  def queryJson(q: FilteredQuery): Task[String \/ APIResults[FedoraPackage]] =
    query(q) ∘ (_.replaceAll("""<\/?.*?>""", "").decodeEither[APIResults[FedoraPackage]])
}
