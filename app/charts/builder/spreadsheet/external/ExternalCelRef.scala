package charts.builder.spreadsheet.external

import charts.builder.spreadsheet.SpreadsheetDataSource
import scala.concurrent.Future
import java.io.InputStream

trait CellLink {
  type Selector = String

  /**
   * Source (ie. cell to copy from) selector
   */
  def source: Selector

  /**
   * Destination (ie. cell to copy to) selector
   */
  def destination: Selector

}

sealed trait ExternalCellRef[S] {
  type SpreadsheetReference = S

  def source: SpreadsheetReference

  def link: CellLink
}

case class UnresolvedRef(val source: String, val link: CellLink)
  extends ExternalCellRef[String]

case class ResolvedRef(val source: SpreadsheetDataSource, val link: CellLink)
  extends ExternalCellRef[SpreadsheetDataSource]


trait ExternalCellRefDetector {

  def scan(ds: SpreadsheetDataSource): Future[Set[ExternalCellRef[_]]]

}


trait ExternalCellRefReplacer {

  // If None is returned, then use original
  def replace(ds: SpreadsheetDataSource, refs: Set[ResolvedRef]):
    Future[Option[InputStream]]

}


trait ExternalCellRefResolver {

  type DestinationIdentifier = String

  def resolve(base: DestinationIdentifier, refs: Set[ExternalCellRef[_]]):
    Future[Set[ResolvedRef]]

}