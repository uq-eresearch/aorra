package charts;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;

public class XlsDataSource implements DataSource {

    private HSSFWorkbook workbook;

    private HSSFFormulaEvaluator evaluator;

    public XlsDataSource(InputStream in) throws Exception {
        workbook = new HSSFWorkbook(in);
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.setIgnoreMissingWorkbooks(true);
    }

    @Override
    public Value select(String selector) throws Exception {
        // currently only CellReference selectors are supported like [sheet!]<row><column>
        // e.g. Coral!A1 or just B20 which will select the cell from the first sheet.
        CellReference cr = new CellReference(selector);
        HSSFSheet sheet;
        String sheetName = cr.getSheetName();
        if(sheetName != null) {
            sheet = getSheet(sheetName);
            if(sheet == null) {
                throw new Exception(String.format("Sheet '%s' does not exist in workbook", sheetName));
            }
        } else {
            sheet = workbook.getSheetAt(0);
            if(sheet == null) {
                throw new Exception(String.format("Sheet does not exist in workbook"));
            }
        }
        Row row = sheet.getRow(cr.getRow());
        if(row == null) {
            throw new Exception(String.format("Row %s does not exists in sheet", selector));
        }
        Cell cell = row.getCell(cr.getCol());
        if(cell == null) {
            throw new Exception(String.format("Cell %s does not exists in sheet", selector));
        }
        return new XlsCellValue(cell, evaluator);
    }

    private HSSFSheet getSheet(String name) {
        HSSFSheet sheet = workbook.getSheet(name);
        String strippedName = StringUtils.strip(name);
        if(sheet == null) {
            for(int i=0;i<workbook.getNumberOfSheets();i++) {
                if(strippedName.equalsIgnoreCase(StringUtils.strip(workbook.getSheetName(i)))) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }
        }
        if(sheet == null) {
            for(int i=0;i<workbook.getNumberOfSheets();i++) {
                if(StringUtils.containsIgnoreCase(StringUtils.strip(workbook.getSheetName(i)), strippedName)) {
                    sheet = workbook.getSheetAt(i);
                    break;
                }
            }
        }
        return sheet;
    }

}
