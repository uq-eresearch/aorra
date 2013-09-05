package charts.builder.spreadsheet;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import charts.builder.DataSource;
import charts.builder.Value;

public abstract class SpreadsheetDataSource implements DataSource {

  private Workbook workbook;

  private FormulaEvaluator evaluator;

  private static class SpreadsheetCellValue implements Value {

    private final Cell cell;

    private final FormulaEvaluator evaluator;

    public SpreadsheetCellValue(Cell cell, FormulaEvaluator evaluator) {
      this.cell = cell;
      this.evaluator = evaluator;
    }

    @Override
    public String format(String pattern) throws Exception {
      if ("value".equals(pattern)) {
        return getValue();
      }
      throw new Exception("unknown pattern " + pattern);
    }

    private String getValue() {
      String result;
      CellValue cellValue = evaluator.evaluate(cell);
      if (cellValue == null) {
        return "";
      }
      switch (cellValue.getCellType()) {
      case Cell.CELL_TYPE_BOOLEAN:
        result = Boolean.toString(cellValue.getBooleanValue());
        break;
      case Cell.CELL_TYPE_NUMERIC:
        double val = cellValue.getNumberValue();
        result = Double.toString(val);
        break;
      case Cell.CELL_TYPE_STRING:
        result = cellValue.getStringValue();
        break;
      case Cell.CELL_TYPE_BLANK:
        result = "";
        break;
      case Cell.CELL_TYPE_ERROR:
        result = ErrorEval.getText(cellValue.getErrorValue());
        break;
      // CELL_TYPE_FORMULA will never happen
      case Cell.CELL_TYPE_FORMULA:
        result = "#FORMULAR";
        break;
      default:
        result = "#DEFAULT";
      }
      return result;
    }

    @Override
    public String toString() {
      String result;
      DataFormatter df = new DataFormatter();
      result = df.formatCellValue(cell, evaluator);
      return result;
    }

    @Override
    public String asString() {
      return getValue();
    }

    @Override
    public Double asDouble() {
      String s = getValue();
      if (StringUtils.isNotBlank(s)) {
        return new Double(s);
      } else {
        return null;
      }
    }

    @Override
    public Integer asInteger() {
      String s = getValue();
      if (StringUtils.isNotBlank(s)) {
        return new Integer(Math.round(Float.parseFloat(s)));
      } else {
        return null;
      }
    }

  }

  private static class EmptyCell implements Value {
    @Override
    public String format(String pattern) throws Exception {
      return null;
    }

    @Override
    public String asString() {
      return null;
    }

    @Override
    public Double asDouble() {
      return null;
    }

    @Override
    public Integer asInteger() {
      return null;
    }
  }

  void init(Workbook workbook, FormulaEvaluator evaluator) {
    this.workbook = workbook;
    this.evaluator = evaluator;
  }

  /**
   * select value from 1st sheet
   *
   * @param row
   *          - starts with 0
   * @param col
   *          - starts with 0
   */
  public Value select(int row, int col) {
    return select(null, row, col);
  }

  public Value select(String sheetname, int row, int col) {
    String cellref = new CellReference(row, col).formatAsString();
    if (StringUtils.isNotBlank(sheetname)) {
      cellref = sheetname + "!" + cellref;
    }
    return select(cellref);
  }

  @Override
  public Value select(String selector) {
    // currently only CellReference selectors are supported like
    // [sheet!]<row><column>
    // e.g. Coral!A1 or just B20 which will select the cell from the first
    // sheet.
    CellReference cr = new CellReference(selector);
    Sheet sheet;
    String sheetName = cr.getSheetName();
    if (sheetName != null) {
      sheet = getSheet(sheetName);
      if (sheet == null) {
        throw new RuntimeException(String.format(
            "Sheet '%s' does not exist in workbook", sheetName));
      }
    } else {
      sheet = workbook.getSheetAt(0);
      if (sheet == null) {
        throw new RuntimeException(
            String.format("Sheet does not exist in workbook"));
      }
    }
    Row row = sheet.getRow(cr.getRow());
    if (row == null) {
      return new EmptyCell();
    }
    Cell cell = row.getCell(cr.getCol());
    if (cell == null) {
      return new EmptyCell();
    }
    return new SpreadsheetCellValue(cell, evaluator);
  }

  private Sheet getSheet(String name) {
    Sheet sheet = workbook.getSheet(name);
    String strippedName = StringUtils.strip(name);
    if (sheet == null) {
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        if (strippedName.equalsIgnoreCase(StringUtils.strip(workbook
            .getSheetName(i)))) {
          sheet = workbook.getSheetAt(i);
          break;
        }
      }
    }
    if (sheet == null) {
      for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        if (StringUtils.containsIgnoreCase(
            StringUtils.strip(workbook.getSheetName(i)), strippedName)) {
          sheet = workbook.getSheetAt(i);
          break;
        }
      }
    }
    return sheet;
  }

}
