package charts.builder.spreadsheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XlsDataSourceTest extends SpreadsheetDataSourceTest {

  @Override
  SpreadsheetDataSource datasource() throws IOException {
    return datasource(new FileInputStream("test/extref.xls"));
  }

  @Override
  SpreadsheetDataSource datasource(InputStream in) throws IOException {
    return new XlsDataSource(in);
  }

}
