package charts.builder.spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.tika.io.IOUtils;

import charts.builder.DataSource;
import charts.builder.Value;
import charts.builder.spreadsheet.external.ResolvedRef;
import charts.builder.spreadsheet.external.SimpleCellLink;
import charts.builder.spreadsheet.external.UnresolvedRef;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class SpreadsheetDataSource implements DataSource {

  private Workbook workbook;

  private FormulaEvaluator evaluator;

  private final int defaultSheet;

  private class SpreadsheetCellValue implements Value {

    private final Cell cell;

    public SpreadsheetCellValue(Cell cell) {
      this.cell = cell;
    }

    @Override
    public String getValue() {
      String result = "";
      try {
        CellValue cellValue = evaluator().evaluate(cell);
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
      } catch(RuntimeException e) {
        if(cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
          switch(cell.getCachedFormulaResultType()) {
            case Cell.CELL_TYPE_NUMERIC:
              double val = cell.getNumericCellValue();
              result = Double.toString(val);
              break;
            case Cell.CELL_TYPE_ERROR:
              FormulaError fe = FormulaError.forInt(cell.getErrorCellValue());
              result = fe.getString();
              break;
            case Cell.CELL_TYPE_STRING:
              result = cell.getStringCellValue();
              break;
            case Cell.CELL_TYPE_BOOLEAN:
              result = Boolean.toString(cell.getBooleanCellValue());
              break;
            default:
              result = "";
          }
        }
      }
      return result;
    }

    @Override
    public String toString() {
      String result;
      DataFormatter df = new DataFormatter();
      result = df.formatCellValue(cell, evaluator());
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

    @Override
    public java.awt.Color asColor() {
      for(Color c : Lists.newArrayList(cell.getCellStyle().getFillForegroundColorColor(),
          cell.getCellStyle().getFillBackgroundColorColor())) {
        if (c instanceof HSSFColor && (((HSSFColor)c).getTriplet() != null)) {
          final short[] rgb = ((HSSFColor)c).getTriplet();
          return new java.awt.Color(rgb[0], rgb[1], rgb[2]);
        }
        if (c instanceof XSSFColor && (((XSSFColor)c).getRgb() != null)) {
          final byte[] rgb = ((XSSFColor)c).getRgb();
          // Convert bytes to unsigned integers
          return new java.awt.Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
        }
      }
      return null;
    }

    @Override
    public Date asDate() {
        return cell.getDateCellValue();
    }

    @Override
    public Double asPercent() {
      Double value = null;
      if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
        if(cell.getCellStyle().getDataFormatString().contains("%")) {
          value = cell.getNumericCellValue();
        } else {
          value = cell.getNumericCellValue() / 100;
        }
      }
      return value;
    }
  }

  private static class EmptyCell implements Value {
    @Override
    public String getValue() {
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

    @Override
    public java.awt.Color asColor() {
      return null;
    }

    @Override
    public Date asDate() {
        return null;
    }

    @Override
    public Double asPercent() {
      return null;
    }

  }

  public SpreadsheetDataSource() {
    defaultSheet = 0;
  }

  SpreadsheetDataSource(Workbook workbook, FormulaEvaluator evaluator, int defaultSheet) {
    this.workbook = workbook;
    this.evaluator = evaluator;
    this.defaultSheet = defaultSheet;
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
   * @throws MissingDataException
   */
  public Value select(int row, int col) throws MissingDataException {
    return select(null, row, col);
  }

  public Value select(String sheetname, int row, int col)
      throws MissingDataException {
    String cellref = new CellReference(row, col).formatAsString();
    if (StringUtils.isNotBlank(sheetname)) {
      cellref = sheetname + "!" + cellref;
    }
    return select(cellref);
  }

  public Value select(String sheetname, String selector) throws MissingDataException {
      return select(sheetname+"!"+selector);
  }

  @Override
  public Value select(String selector) throws MissingDataException {
    Cell cell = selectCell(selector);
    return cell!=null?new SpreadsheetCellValue(cell):new EmptyCell();
  }

  private Cell selectCell(String selector) throws MissingDataException {
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
        throw new MissingDataException(String.format(
            "Sheet '%s' does not exist in workbook", sheetName));
      }
    } else {
      sheet = workbook.getSheetAt(defaultSheet);
      if (sheet == null) {
        throw new MissingDataException(
            String.format("Sheet does not exist in workbook"));
      }
    }
    Row row = sheet.getRow(cr.getRow());
    if (row == null) {
      return null;
    }
    Cell cell = row.getCell(cr.getCol());
    if (cell == null) {
      return null;
    }
    return cell;
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

  public boolean hasSheet(String name) {
      return getSheet(name) != null;
  }

  public String getSheetname(int i) {
      Sheet sheet = workbook.getSheetAt(i);
      if(sheet != null) {
          return sheet.getSheetName();
      } else {
          return null;
      }
  }

  public int sheets() {
      return workbook.getNumberOfSheets();
  }

  public abstract SpreadsheetDataSource toSheet(int sheet);

  public SpreadsheetDataSource toSheet(String sheetname) {
    Sheet s = getSheet(sheetname);
    if(s!= null) {
      return toSheet(workbook.getSheetIndex(s));
    } else {
      return null;
    }
  }

  public String getDefaultSheet() {
      return workbook.getSheetName(defaultSheet);
  }

  public Integer getColumnCount(int row) {
      return getColumnCount(defaultSheet, row);
  }

  public Integer getColumnCount(int i, int row) {
      Sheet sheet = workbook.getSheetAt(i);
      if(sheet != null) {
          Row r = sheet.getRow(row);
          if(r != null) {
              return Integer.valueOf(r.getLastCellNum());
          }
      }
      return null;
  }

  public List<Value> selectRow(int row) throws MissingDataException {
      List<Value> result = Lists.newArrayList();
      Integer max = getColumnCount(row);
      if(max == null) {
          return result;
      }
      for(int col = 0;col <= max;col++) {
          result.add(select(row, col));
      }
      return result;
  }

  public List<Value> selectColumn(int column) throws MissingDataException {
      return selectColumn(column, 100);
  }

  public List<Value> selectColumn(int column, int limit) throws MissingDataException {
      List<Value> result = Lists.newArrayList();
      Sheet sheet = workbook.getSheetAt(defaultSheet);
      int max = Math.min(sheet.getLastRowNum(), limit);
      for(int row = 0; row <= max;row++) {
          result.add(select(row, column));
      }
      return result;
  }

  public static boolean containsString(List<Value> values, String s) {
      for(Value v : values) {
          if(StringUtils.equals(v.asString(),s)) {
              return true;
          }
      }
      return false;
  }

  Workbook workbook() {
    return workbook;
  }

  FormulaEvaluator evaluator() {
    return evaluator;
  }

  public boolean hasExternalReferences() {
    for (int si = 0; si < workbook.getNumberOfSheets();si++) {
      Sheet sheet = workbook.getSheetAt(si);
      for (Row row : sheet) {
        for (Cell cell : row) {
          if (externalReference(cell) != null) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public Set<UnresolvedRef> externalReferences() {
    Set<UnresolvedRef> urefs = Sets.newHashSet();
    for(int si = 0; si < workbook.getNumberOfSheets();si++) {
      Sheet sheet = workbook.getSheetAt(si);
      for(Row row : sheet) {
        for(Cell cell : row) {
          UnresolvedRef uref = externalReference(cell);
          if(uref != null) {
            urefs.add(uref);
          }
        }
      }
    }
    return urefs;
  }

  abstract UnresolvedRef externalReference(Cell cell);

  UnresolvedRef uref(String sIdOrName, final String sSelector,
      final String dSelector) {
    return new UnresolvedRef(sIdOrName,
        new SimpleCellLink(sSelector, dSelector));
  }

  public InputStream updateExternalReferences(Set<ResolvedRef> refs) throws IOException {
    boolean dirty = false;
    for (ResolvedRef ref : refs) {
      try {
        final Cell dCell = selectCell(ref.link().destination());
        if (dCell == null)
          continue;
        if (ref.source().isDefined()) {
          final SpreadsheetDataSource source = ref.source().get();
          final Cell sCell = source.selectCell(ref.link().source());
          dirty |= updatePrecalculatedValue(dCell, sCell, source.evaluator());
        } else {
          dirty |= updatePrecalculatedError(dCell, FormulaError.REF);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    evaluator().evaluateAll();
    return dirty ? writeToTempFile() : null;
  }

  private boolean updatePrecalculatedValue(Cell destination,
      Cell source, FormulaEvaluator sEvaluator) {
    if(source != null) {
      switch(source.getCellType()) {
      case Cell.CELL_TYPE_BLANK:
        return updatePrecalculatedBlank(destination);
      case Cell.CELL_TYPE_BOOLEAN:
        return updatePrecalculatedBoolean(destination, source.getBooleanCellValue());
      case Cell.CELL_TYPE_ERROR:
        return updatePrecalculatedError(destination,
            FormulaError.forInt(source.getErrorCellValue()));
      case Cell.CELL_TYPE_FORMULA:
        try {
          return updatePrecalculatedCellValue(destination, sEvaluator.evaluate(source));
        } catch(Exception e) {
          switch(source.getCachedFormulaResultType()) {
          case Cell.CELL_TYPE_NUMERIC:
            return updatePrecalculatedNumeric(destination, source.getNumericCellValue());
          case Cell.CELL_TYPE_STRING:
            return updatePrecalculatedString(destination, source.getStringCellValue());
          case Cell.CELL_TYPE_BOOLEAN:
            return updatePrecalculatedBoolean(destination, source.getBooleanCellValue());
          case Cell.CELL_TYPE_ERROR:
            return updatePrecalculatedError(destination,
                FormulaError.forInt(source.getErrorCellValue()));
          }
        }
      case Cell.CELL_TYPE_NUMERIC:
        return updatePrecalculatedNumeric(destination, source.getNumericCellValue());
      case Cell.CELL_TYPE_STRING:
        return updatePrecalculatedString(destination, source.getStringCellValue());
      default:
        return false;
      }
    } else {
      return updatePrecalculatedError(destination, FormulaError.REF);
    }
  }

  private boolean updatePrecalculatedCellValue(Cell destination, CellValue val) {
    if(val != null) {
      switch(val.getCellType()) {
      case Cell.CELL_TYPE_BOOLEAN:
        return updatePrecalculatedBoolean(destination, val.getBooleanValue());
      case Cell.CELL_TYPE_NUMERIC:
        return updatePrecalculatedNumeric(destination, val.getNumberValue());
      case Cell.CELL_TYPE_STRING:
        return updatePrecalculatedString(destination, val.getStringValue());
      case Cell.CELL_TYPE_BLANK:
        return updatePrecalculatedBlank(destination);
      case Cell.CELL_TYPE_ERROR:
        return updatePrecalculatedError(destination,
            FormulaError.forInt(val.getErrorValue()));
      default: return false;
      }
    } else {
      return updatePrecalculatedError(destination, FormulaError.REF);
    }
  }

  private boolean updatePrecalculatedBlank(Cell destination) {
    return updatePrecalculatedNumeric(destination, 0);
  }

  private boolean updatePrecalculatedNumeric(Cell destination, double sVal) {
    if(isFormula(destination)) {
      try {
        double dVal = destination.getNumericCellValue();
        if(dVal != sVal) {
          destination.setCellValue(sVal);
          return true;
        }
      } catch(Exception e) {
        destination.setCellValue(sVal);
        return true;
      }
    }
    return false;
  }

  private boolean updatePrecalculatedString(Cell destination, String sVal) {
    if(isFormula(destination)) {
      try {
        String dVal = destination.getStringCellValue();
        if(!StringUtils.equals(sVal, dVal)) {
          destination.setCellValue(sVal);
          return true;
        }
      } catch(Exception e) {
        destination.setCellValue(sVal);
        return true;
      }
    }
    return false;
  }

  private boolean updatePrecalculatedError(Cell destination, FormulaError sError) {
    if(isFormula(destination)) {
      try {
        FormulaError dError = FormulaError.forInt(destination.getErrorCellValue());
        if(sError != dError) {
          destination.setCellErrorValue(sError.getCode());
          return true;
        }
      } catch(Exception e) {
        destination.setCellValue(sError.getCode());
        return true;
      }
    }
    return false;
  }

  private boolean updatePrecalculatedBoolean(Cell destination, boolean sVal) {
    if(isFormula(destination)) {
      try {
        boolean dVal = destination.getBooleanCellValue();
        if(dVal != sVal) {
          destination.setCellValue(sVal);
          return true;
        }
      } catch(Exception e) {
        destination.setCellValue(sVal);
        return true;
      }
    }
    return false;
  }

  private boolean isFormula(Cell cell) {
    return (cell != null) && (cell.getCellType() == Cell.CELL_TYPE_FORMULA);
  }

  private InputStream writeToTempFile() throws IOException {
    final File f = File.createTempFile("spreadsheet", "poi");
    FileOutputStream out = new FileOutputStream(f);
    workbook.write(out);
    IOUtils.closeQuietly(out);
    return new FileInputStream(f) {
      @Override
      public void close() throws IOException {
        super.close();
        FileUtils.deleteQuietly(f);
      }
    };
  }

}
