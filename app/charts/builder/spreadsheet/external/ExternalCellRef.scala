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

  override def toString = s""""$source" \u2192 "$destination""""

}

class SimpleCellLink(val source: String, val destination: String)
  extends CellLink

sealed trait ExternalCellRef[S] {
  type SpreadsheetReference = S

  def source: SpreadsheetReference

  def link: CellLink
}

case class UnresolvedRef(val source: String, val link: CellLink)
  extends ExternalCellRef[String]

case class ResolvedRef(
    val source: Option[SpreadsheetDataSource],
    val link: CellLink) extends ExternalCellRef[Option[SpreadsheetDataSource]]


trait ExternalCellRefDetector {

  def hasAny(ds: SpreadsheetDataSource): Boolean

  def scan(ds: SpreadsheetDataSource): Set[UnresolvedRef]

}


trait ExternalCellRefReplacer {

  // If None is returned, then use original
  def replace(ds: SpreadsheetDataSource, refs: Set[ResolvedRef]):
    Option[InputStream]

}


trait ExternalCellRefResolver {

  type DestinationIdentifier = String

  def resolve(base: DestinationIdentifier, refs: Set[UnresolvedRef]):
    Set[ResolvedRef]

}