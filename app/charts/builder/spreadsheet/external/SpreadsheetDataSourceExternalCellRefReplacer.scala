package charts.builder.spreadsheet.external

import charts.builder.spreadsheet.SpreadsheetDataSource
import java.io.InputStream
import scala.collection.JavaConversions.setAsJavaSet

object SpreadsheetDataSourceExternalCellRefReplacer
  extends ExternalCellRefReplacer {

  // If None is returned, then use original
  override def replace(ds: SpreadsheetDataSource, refs: Set[ResolvedRef]) =
      Option(ds.updateExternalReferences(setAsJavaSet(refs)))


}