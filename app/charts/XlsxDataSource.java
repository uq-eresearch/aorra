package charts;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XlsxDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(XlsxDataSource.class);

    private XSSFWorkbook workbook;

    private FormulaEvaluator evaluator;

    public XlsxDataSource(InputStream in) throws Exception {
        workbook = new XSSFWorkbook(in);
        evaluator = workbook.getCreationHelper().createFormulaEvaluator();
    }

    @Override
    public Value select(String selector) throws Exception {
        // currently only CellReference selectors are supported like [sheet!]<row><column>
        // e.g. Coral!A1 or just B20 which will select the cell from the first sheet.
        logger.debug(String.format("in select(\"%s\")", selector));
        CellReference cr = new CellReference(selector);
        XSSFSheet sheet;
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
        logger.debug(String.format("sheet name: %s", sheet));
        Row row = sheet.getRow(cr.getRow());
        if(row == null) {
            throw new Exception(String.format("Row %s does not exists in sheet", selector));
        }
        Cell cell = row.getCell(cr.getCol());
        if(cell == null) {
            throw new Exception(String.format("Cell %s does not exists in sheet", selector));
        }
        return new XlsxCellValue(cell, evaluator);
    }

    private XSSFSheet getSheet(String name) {
        XSSFSheet sheet = workbook.getSheet(name);
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

    public static void main(String[] args) throws Exception {
        File file = new File("/home/uqageber/workspace/ereefs/local/repo/content/Marine Scores 09_10.xlsx");
        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file));
        System.out.println(workbook.getNumberOfSheets());
        for(int i=0;i<workbook.getNumberOfSheets();i++) {
            System.out.println(String.format("'%s'",workbook.getSheetName(i)));
        }
        Sheet sheet = workbook.getSheetAt(1);
        System.out.println(sheet);
        System.out.println("Summary ".matches(".*ar.*"));
    }

}
