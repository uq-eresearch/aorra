package charts;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XlsxCellValue implements Value {

    private static final Logger logger = LoggerFactory.getLogger(XlsxCellValue.class);

    private Cell cell;

    private FormulaEvaluator evaluator;

    public XlsxCellValue(Cell cell, FormulaEvaluator evaluator) {
        this.cell = cell;
        this.evaluator = evaluator;
    }

    @Override
    public String format(String pattern) throws Exception {
        if("value".equals(pattern)) {
            return getValue();
        }
        throw new Exception("unknown pattern "+pattern);
    }

    private String getValue() {
        String result;
        CellValue cellValue = evaluator.evaluate(cell);
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
        if(logger.isDebugEnabled()) {
            logger.debug(String.format("data format string '%s' and index '%s'",
                    cell.getCellStyle().getDataFormatString(), cell.getCellStyle().getDataFormat()));
        }
        DataFormatter df = new DataFormatter();
        result = df.formatCellValue(cell, evaluator);
        return result;
    }

}
