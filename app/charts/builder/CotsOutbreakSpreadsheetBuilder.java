package charts.builder;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;

import charts.CotsOutbreak;
import charts.Drawable;
import charts.spreadsheet.DataSource;

public class CotsOutbreakSpreadsheetBuilder extends DefaultSpreadsheetChartBuilder {

    public CotsOutbreakSpreadsheetBuilder() {
        super(ChartType.COTS_OUTBREAK);
    }

    private boolean isCotsOutbreakSpreadsheet(DataSource datasource) {
        try {
            return "TOTALOUTBREAKS".equalsIgnoreCase(datasource.select("B1").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    private XYDataset createDataset(DataSource datasource) {
        int i = 2;
        TimeSeries s1 = new TimeSeries("outbreaks");
        while(true) {
            try {
                String year = datasource.select("A"+i).format("value");
                String outbreaks = datasource.select("B"+i).format("value");
                if(StringUtils.isNotBlank(year) && StringUtils.isNotBlank(outbreaks)) {
                    double val = Double.parseDouble(outbreaks);
                    s1.add(new Year(parseYear(year)), val);
                } else {
                    break;
                }
                i++;
            } catch(Exception e) {
                break;
            }
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        return dataset;
    }

    private int parseYear(String y) {
        y = StringUtils.strip(y);
        if(y.contains(".")) {
            y = StringUtils.substringBefore(y, ".");
        }
        return Integer.parseInt(y);
    }

    @Override
    boolean canHandle(DataSource datasource) {
        return isCotsOutbreakSpreadsheet(datasource);
    }

    @Override
    Chart build(DataSource datasource, Region region, Map<String, String[]> query) {
        if(region.equals(Region.GBR)) {
            XYDataset dataset = createDataset(datasource);
            Drawable d = new CotsOutbreak().createChart(dataset, getChartSize(query, 750, 500));
            Chart chart = new Chart(new ChartDescription(ChartType.COTS_OUTBREAK, region), d);
            return chart;
        } else {
            return null;
        }
    }

}
