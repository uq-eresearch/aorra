package charts.builder;

import java.util.Map;

import charts.ProgressTable;
import charts.spreadsheet.DataSource;

public class ProgressTableChartBuilder extends DefaultSpreadsheetChartBuilder {

    public ProgressTableChartBuilder() {
        super(ChartType.PROGRESS_TABLE);
    }

    @Override
    boolean canHandle(DataSource datasource) {
        try {
            return "progress towards targets indicators".equalsIgnoreCase(
                    datasource.select("A1").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    Chart build(DataSource datasource, Region region,
            Map<String, String[]> query) {
        if(region == Region.GBR) {
            return new Chart(
                new ChartDescription(ChartType.PROGRESS_TABLE, Region.GBR), new ProgressTable());
        }
        return null;
    }

}
