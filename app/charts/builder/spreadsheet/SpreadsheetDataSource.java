package charts.builder.spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.tika.io.IOUtils;

import play.Logger;
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
      return asString();
    }

    @Override
    public String asString() {
      return getValue();
    }

    @Override
    public Double asDouble() {
      String s = getValue();
      try {
        return new Double(s);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    @Override
    public Integer asInteger() {
      String s = getValue();
      try {
        return new Integer(Math.round(Float.parseFloat(s)));
      } catch (NumberFormatException e) {
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
      try {
        return cell.getDateCellValue();
      } catch(Exception e) {
        final String s = getValue();
        // TODO it would be better if we could somehow parse an arbitrary date format
        // http://stackoverflow.com/questions/3850784/recognise-an-arbitrary-date-string
        // http://stackoverflow.com/questions/3389348/parse-any-date-in-java
        final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
          return sdf.parse(s);
        } catch(Exception e2) {
          throw e;
        }
      }
    }

    @Override
    public Double asPercent() {
      Double value = asDouble();
      if(!cell.getCellStyle().getDataFormatString().contains("%") && (value!=null)) {
        value = value / 100.0;
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
            //Logger.debug(String.format(
            //    "found external reference source '%s', source cell '%s', destination cell '%s'",
            //    uref.source(), uref.link().source(), uref.link().destination()));
            urefs.add(uref);
          }
        }
      }
    }
    return urefs;
  }

  abstract UnresolvedRef externalReference(Cell cell);

  protected UnresolvedRef uref(String sIdOrName, final String sSelector,
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
          try {
            final Cell sCell = source.selectCell(ref.link().source());
            dirty |= updatePrecalculatedValue(dCell, sCell, source.evaluator());
          } catch (MissingDataException e) {
            dirty |= updatePrecalculatedError(dCell, FormulaError.REF);
          }
        } else {
          dirty |= updatePrecalculatedError(dCell, FormulaError.REF);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      evaluateAll();
    } catch(RuntimeException e) {
      Logger.debug("evaluateAll() failed on updateExternalReferences," +
          "some cached formula results may be out of date", e);
    }
    return dirty ? writeToTempFile() : null;
  }

  // evaluate all formula cells but external references. the XSSF evaluator seem to have
  // issues with external references even if setIgnoreMissingWorkbooks(...) is set to true
  // this workaround will probably not fully resolve the issue as formulas that depend on
  // problematic external references might still fail.
  private void evaluateAll() {
    for(int i=0; i<workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      for(Row r : sheet) {
        for (Cell c : r) {
          if (c.getCellType() == Cell.CELL_TYPE_FORMULA && !isExternalReference(c)) {
            try {
              evaluator.evaluateFormulaCell(c);
            } catch(RuntimeException e) {
              CellReference cr = new CellReference(c);
              Logger.debug(String.format("failed to evaluate cell %s!%s, formula %s." +
                  " some cached formula results may be out of date",
                  sheet.getSheetName(), cr.formatAsString(), c.getCellFormula()), e);
            }
          }
        }
      }
    }
  }

  private boolean isExternalReference(Cell cell) {
    return externalReference(cell) != null;
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
        destination.setCellErrorValue(sError.getCode());
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

  public int getColumns(int row) {
    return workbook.getSheetAt(defaultSheet).getRow(row).getLastCellNum();
  }

  public int getRows() {
    return workbook.getSheetAt(defaultSheet).getLastRowNum();
  }

  public Iterable<Value> rangeSelect(final int row1, final int column1,
      final int row2, final int column2) {
    if(row1 == row2) {
      return rangeColumnSelect(row1, column1, column2);
    } else if(column1 == column2 ) {
      return rangeRowSelect(column1, row1, row2);
    } else {
      throw new IllegalArgumentException("can only select from 1 row or 1 column");
    }
  }

  public Iterable<Value> rangeRowSelect(final int column, final int row1, final int row2) {
    return new Iterable<Value>() {
      @Override
      public Iterator<Value> iterator() {
        return rangeIterator(row1, row2, new ValueSelector() {
          @Override
          public Value select(int i) throws MissingDataException {
            return SpreadsheetDataSource.this.select(i, column);
          }});
      }};
  }

  public Iterable<Value> rangeColumnSelect(final int row, final int column1, final int column2) {
    return new Iterable<Value>() {
      @Override
      public Iterator<Value> iterator() {
        return rangeIterator(column1, column2, new ValueSelector() {
          @Override
          public Value select(int i) throws MissingDataException {
            return SpreadsheetDataSource.this.select(row, i);
          }});
      }};
  }

  private interface ValueSelector {
    public Value select(int i) throws MissingDataException;
  }

  private Iterator<Value> rangeIterator(final int from, final int to,
      final ValueSelector selector) {
    return new Iterator<Value> () {
      boolean hasNext = true;
      int i = from;
      @Override
      public boolean hasNext() {
        return hasNext;
      }

      @Override
      public Value next() {
        if(hasNext) {
          hasNext = !(i == to);
          try {
            return selector.select(i);
          } catch(MissingDataException e) {
            return new EmptyCell();
          } finally {
            i += from < to ? 1 : -1;
          }
        } else {
          throw new NoSuchElementException();
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }};
  }

}
