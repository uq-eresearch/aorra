package charts.builder.spreadsheet.external

import org.specs2.mutable.Specification
import java.io.FileInputStream
import charts.builder.FileStoreDataSourceFactory.getDataSource
import charts.builder.spreadsheet.XlsxDataSource
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
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

    "be able to detect the presence of external refs" in {
      val expectations = List(
        ("test/marine.xls" -> false),
        ("test/marine.xlsx" -> false),
        ("test/extref.xlsx" -> true),
        ("test/progress_table.xlsx" -> true)
      )

      for {
        (filename, expected) <- expectations
      } {
        val ds = getDataSource(filename)
        val result = Await.result(
              subject.hasAny(ds),
              Duration(30, TimeUnit.SECONDS))
        result must_== expected
      }
      true
    }

  }

}