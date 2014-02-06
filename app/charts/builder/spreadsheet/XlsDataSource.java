package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import charts.builder.spreadsheet.external.UnresolvedRef;

public class XlsDataSource extends SpreadsheetDataSource {

  private static final Pattern EXTERNAL_REF_FORMULA = Pattern.compile(
      "^\\+'?\\[(.+)\\](.*?)'?!\\$(\\w+)\\$(\\d+)$");

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
  UnresolvedRef externalReference(Cell cell, String sheetname) {
    UnresolvedRef uref = null;
    if((cell != null) && (cell.getCellType() == Cell.CELL_TYPE_FORMULA)) {
      Matcher m = EXTERNAL_REF_FORMULA.matcher(cell.getCellFormula());
      if(m.matches()) {
        CellReference crSource = new CellReference(String.format("%s!%s%s",
            m.group(2), m.group(3), m.group(4)));
        uref = uref(m.group(1),
            crSource.formatAsString(), String.format("%s!%s", sheetname,
            new CellReference(cell).formatAsString()));
      }
    }
    return uref;
  }

}
