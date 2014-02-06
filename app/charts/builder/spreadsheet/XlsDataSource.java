package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import charts.builder.spreadsheet.external.UnresolvedRef;

public class XlsDataSource extends SpreadsheetDataSource {

  public XlsDataSource(InputStream in) throws IOException {
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

  @Override
  public Set<UnresolvedRef> externalReferences() {
    //FIXME implement
    throw new RuntimeException("not implemented");
  }

}
