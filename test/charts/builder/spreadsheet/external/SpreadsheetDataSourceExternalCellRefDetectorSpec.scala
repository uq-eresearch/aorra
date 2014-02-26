package charts.builder.spreadsheet.external

import org.specs2.mutable.Specification
import java.io.FileInputStream
import charts.builder.FileStoreDataSourceFactory.getDataSource
import charts.builder.spreadsheet.XlsxDataSource
import charts.builder.spreadsheet.XlsDataSource

class SpreadsheetDataSourceExternalCellRefDetectorSpec extends Specification {

  val subject = SpreadsheetDataSourceExternalCellRefDetector

  def getDataSource(filename: String) = filename match {
    case _ if filename.endsWith(".xls") =>
      new XlsDataSource(new FileInputStream(filename))
    case _ if filename.endsWith(".xlsx") =>
      new XlsxDataSource(new FileInputStream(filename))
  }

  "detector" should {

    "detect the presence of external refs" in {
      val expectations = List(
        ("test/marine.xls" -> true),
        ("test/marine.xlsx" -> false),
        ("test/extref.xlsx" -> true),
        ("test/progress_table.xlsx" -> true)
      )

      for {
        (filename, expected) <- expectations
      } {
        val ds = getDataSource(filename)
        subject.hasAny(ds) must_== expected
      }
      true
    }

    "provide a set of external refs" in {
      val ds = getDataSource("test/extref.xlsx")
      val refs = subject.scan(ds)
      refs must have size(6)
      val filenames = refs.map(_.source)
      filenames must contain("progress1.xlsx")
      filenames must contain("progress2.xlsx")
      filenames must contain("progress3.xlsx")
      filenames must contain("groundcover below 50%.xlsx")
    }

  }

}