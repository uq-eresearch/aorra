package charts.builder;

import java.util.Map;

import charts.Drawable;
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
          return new AbstractChart(query) {
            @Override
            public ChartDescription getDescription() {
              return new ChartDescription(ChartType.PROGRESS_TABLE, Region.GBR);
            }
            @Override
            public Drawable getChart() {
              return new ProgressTable();
            }
            @Override
            public String getCSV() throws UnsupportedFormatException {
              throw new Chart.UnsupportedFormatException();
            }
          };
        }
        return null;
    }

}
