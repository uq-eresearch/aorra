package charts.builder.spreadsheet;

import java.io.InputStream;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XlsxDataSource extends SpreadsheetDataSource {

  public XlsxDataSource(InputStream in) throws Exception {
    XSSFWorkbook workbook = new XSSFWorkbook(in);
    FormulaEvaluator evaluator = workbook.getCreationHelper()
        .createFormulaEvaluator();
    init(workbook, evaluator);
  }

  private XlsxDataSource(Workbook workbook, FormulaEvaluator evaluator, int defaultSheet) {
    super(workbook, evaluator, defaultSheet);
  }

  @Override
  public SpreadsheetDataSource toSheet(int sheet) {
    return new XlsxDataSource(workbook(), evaluator(), sheet);
  }
}
