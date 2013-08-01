package charts;

import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

public class XlsCellValue implements Value {

    private Cell cell;

    private FormulaEvaluator evaluator;

    public XlsCellValue(Cell cell, FormulaEvaluator evaluator) {
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
        if(cellValue == null) {
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
}
