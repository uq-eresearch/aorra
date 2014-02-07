package charts.builder.spreadsheet.external

import charts.builder.spreadsheet.SpreadsheetDataSource
import scala.collection.JavaConversions.asScalaSet

object SpreadsheetDataSourceExternalCellRefDetector extends ExternalCellRefDetector {

  def hasAny(ds: SpreadsheetDataSource): Boolean =
    ds.hasExternalReferences

  def scan(ds: SpreadsheetDataSource): Set[UnresolvedRef] =
    asScalaSet(ds.externalReferences).toSet

}