package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import charts.builder.spreadsheet.external.UnresolvedRef;

public class XlsDataSource extends SpreadsheetDataSource {

  private static final Pattern EXTERNAL_REF_FORMULA = Pattern.compile(
      "^\\+?('?)\\[(.+)\\](.*?)'?!\\$?([A-Za-z]+)\\$?(\\d+)$");

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
  UnresolvedRef externalReference(Cell cell) {
    UnresolvedRef uref = null;
    if((cell != null) && (cell.getCellType() == Cell.CELL_TYPE_FORMULA)) {
      Matcher m = EXTERNAL_REF_FORMULA.matcher(cell.getCellFormula());
      if(m.matches()) {
        String nameOrId = m.group(2);
        String sheetname = m.group(3);
        String column = m.group(4);
        String row = m.group(5);
        if(StringUtils.isNotBlank(m.group(1))) {
          sheetname = unescapeSheetname(sheetname);
        }
        uref = uref(nameOrId,
            String.format("%s!%s%s", sheetname, column, row),
            String.format("%s!%s", cell.getSheet().getSheetName(),
            new CellReference(cell).formatAsString()));
      }
    }
    return uref;
  }

  private String unescapeSheetname(String name) {
    return StringUtils.replace(name, "''", "'");
  }

}
