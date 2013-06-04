package org.fedoraproject.mobile

import spray.json._

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

object JSONParsing extends DefaultJsonProtocol {
  implicit val filteredQueryFormat = jsonFormat(FilteredQuery, "rows_per_page", "start_row", "filters")
  implicit val packageFormat: JsonFormat[Package] = lazyFormat(jsonFormat(Package, "icon", "description", "link", "sub_pkgs", "summary", "name", "upstream_url", "devel_owner"))
  implicit val packageResultFormat = jsonFormat(APIResults[Package], "visible_rows", "total_rows", "rows_per_page", "start_row", "rows")
}
