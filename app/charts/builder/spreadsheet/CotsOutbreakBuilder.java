package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.Year;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.CotsOutbreak;
import charts.Drawable;
import charts.builder.AbstractChart;
import charts.builder.Chart;
import charts.builder.ChartDescription;
import charts.builder.ChartType;
import charts.builder.Region;

public class CotsOutbreakBuilder extends AbstractBuilder {

  public CotsOutbreakBuilder() {
    super(ChartType.COTS_OUTBREAK);
  }

  private boolean isCotsOutbreakSpreadsheet(SpreadsheetDataSource datasource) {
    try {
      return "TOTALOUTBREAKS".equalsIgnoreCase(datasource.select("B1").format(
          "value"));
    } catch (Exception e) {
      return false;
    }
  }

  private TimeSeriesCollection createDataset(SpreadsheetDataSource datasource) {
    TimeSeries s1 = new TimeSeries("outbreaks");
    try {
      for (int i = 2; true; i++) {
        String year = datasource.select("A" + i).format("value");
        String outbreaks = datasource.select("B" + i).format("value");
        if (StringUtils.isBlank(year) || StringUtils.isBlank(outbreaks))
          break;
        double val = Double.parseDouble(outbreaks);
        s1.add(new Year(parseYear(year)), val);
      }
    } catch (Exception e) {
    }
    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(s1);
    return dataset;
  }

  private int parseYear(String y) {
    y = StringUtils.strip(y);
    if (y.contains(".")) {
      y = StringUtils.substringBefore(y, ".");
    }
    return Integer.parseInt(y);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    return isCotsOutbreakSpreadsheet(datasource);
  }

  @Override
  public Chart build(SpreadsheetDataSource datasource, ChartType type,
      final Region region, final Map<String, String[]> query) {
    if (!region.equals(Region.GBR)) {
      return null;
    }
    final TimeSeriesCollection dataset = createDataset(datasource);
    final Chart chart = new AbstractChart(query) {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(ChartType.COTS_OUTBREAK, region);
      }

      @Override
      public Drawable getChart() {
        return new CotsOutbreak().createChart(dataset,
            getChartSize(query, 750, 500));
      }

      @SuppressWarnings("unchecked")
      @Override
      public String getCSV() {
        final StringWriter sw = new StringWriter();
        try {
          final CsvListWriter csv = new CsvListWriter(sw,
              CsvPreference.STANDARD_PREFERENCE);
          final DateFormat yearOnly = new SimpleDateFormat("YYYY");
          csv.write("Year", "Outbreaks");
          final List<TimeSeriesDataItem> items = dataset.getSeries(0)
              .getItems();
          for (TimeSeriesDataItem i : items) {
            csv.write(yearOnly.format(i.getPeriod().getStart()), i.getValue()
                .intValue() + "");
          }
          csv.close();
        } catch (IOException e) {
          // How on earth would you get an IOException with a StringWriter?
          throw new RuntimeException(e);
        }
        return sw.toString();
      }
    };
    return chart;
  }

}
