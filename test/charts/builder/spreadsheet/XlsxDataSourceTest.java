package charts.builder.spreadsheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XlsxDataSourceTest extends SpreadsheetDataSourceTest {

  @Override
  SpreadsheetDataSource datasource() throws IOException {
    return datasource(new FileInputStream("test/extref.xlsx"));
  }

  @Override
  SpreadsheetDataSource datasource(InputStream in) throws IOException {
    return new XlsxDataSource(in);
  }

}
