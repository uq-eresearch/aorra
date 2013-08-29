package charts.spreadsheet;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.util.CellReference;

public class SpreadsheetHelper {

    private DataSource datasource;

    public SpreadsheetHelper(DataSource datasource) {
        this.datasource = datasource;
    }

    /**
     * select value from 1st sheet
     * @param row - starts with 0
     * @param col - starts with 0
     */
    public String selectText(int row, int col) {
        return selectText(null, row, col);
    }

    public String selectText(String sheetname, int row, int col) {
        String cellref = new CellReference(row, col).formatAsString();
        if(StringUtils.isNotBlank(sheetname)) {
            cellref = sheetname + "!" + cellref;
        }
        return selectText(cellref);
    }

    public String selectText(String selector) {
        try {
            return datasource.select(selector).format("value");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
