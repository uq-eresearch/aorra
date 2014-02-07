package charts.builder.spreadsheet.external

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future}
import charts.builder.spreadsheet.SpreadsheetDataSource
import scala.collection.JavaConversions.asScalaSet

object SpreadsheetDataSourceExternalCellRefDetector extends ExternalCellRefDetector {

  def hasAny(ds: SpreadsheetDataSource): Future[Boolean] = future {
    !ds.externalReferences.isEmpty
  }

  def scan(ds: SpreadsheetDataSource): Future[Set[UnresolvedRef]] = future {
    asScalaSet(ds.externalReferences).toSet
  }

}