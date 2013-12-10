package charts.builder.spreadsheet;

import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

public class XlsDataSource extends SpreadsheetDataSource {

  public XlsDataSource(InputStream in) throws Exception {
    HSSFWorkbook workbook = new HSSFWorkbook(in);
    HSSFFormulaEvaluator evaluator = workbook.getCreationHelper()
        .createFormulaEvaluator();
    evaluator.setIgnoreMissingWorkbooks(true);
    init(workbook, evaluator);
  }

  private XlsDataSource(Workbook workbook, FormulaEvaluator evaluator, int defaultSheet) {
    super(workbook, evaluator, defaultSheet);
  }

  @Override
  public SpreadsheetDataSource toSheet(int sheet) {
    return new XlsDataSource(workbook(), evaluator(), sheet);
  }

}
