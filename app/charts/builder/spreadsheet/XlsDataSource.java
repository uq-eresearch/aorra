package charts.builder.spreadsheet;

import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class XlsDataSource extends SpreadsheetDataSource {

  public XlsDataSource(InputStream in) throws Exception {
    HSSFWorkbook workbook = new HSSFWorkbook(in);
    HSSFFormulaEvaluator evaluator = workbook.getCreationHelper()
        .createFormulaEvaluator();
    evaluator.setIgnoreMissingWorkbooks(true);
    init(workbook, evaluator);
  }
}
