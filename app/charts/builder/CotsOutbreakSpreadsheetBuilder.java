package charts.builder;

import static charts.builder.ChartBuilder.getFloat;
import static charts.builder.ChartBuilder.renderPNG;
import static charts.builder.ChartBuilder.renderSVG;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;

import charts.CotsOutbreak;
import charts.representations.Format;
import charts.representations.Representation;
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
    Chart build(DataSource datasource, final Region region,
        final Map<String, String[]> query) {
      if (region.equals(Region.GBR)) {
        XYDataset dataset = createDataset(datasource);
        final JFreeChart jfreechart = new CotsOutbreak().createChart(dataset);
        final Chart chart = new Chart() {
          @Override
          public ChartDescription getDescription() {
            return new ChartDescription(ChartType.COTS_OUTBREAK, region);
          }
          @Override
          public Representation outputAs(Format format)
              throws UnsupportedFormatException {
            switch (format) {
            case CSV:
              // TODO: Replace with real implementation
              return format.createRepresentation("");
            case SVG:
              return format.createRepresentation(
                  renderSVG(createDimensions(jfreechart, query)));
            case PNG:
              return format.createRepresentation(
                  renderPNG(createDimensions(jfreechart, query),
                      getFloat(query.get("width")),
                      getFloat(query.get("height"))));
            }
            throw new Chart.UnsupportedFormatException();
          }
        };
        return chart;
      } else {
        return null;
      }
    }

}
