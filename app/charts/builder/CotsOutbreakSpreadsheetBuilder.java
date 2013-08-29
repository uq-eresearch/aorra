package charts.builder;

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
import org.jfree.data.xy.XYDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.CotsOutbreak;
import charts.Drawable;
import charts.BeerCoaster.Category;
import charts.BeerCoaster.Indicator;
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

    private TimeSeriesCollection createDataset(DataSource datasource) {
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
        final TimeSeriesCollection dataset = createDataset(datasource);
        final Drawable drawable = new CotsOutbreak().createChart(dataset,
            getChartSize(query, 750, 500));
        final Chart chart = new AbstractChart(query) {
          @Override
          public ChartDescription getDescription() {
            return new ChartDescription(ChartType.COTS_OUTBREAK, region);
          }
          @Override
          public Drawable getChart() {
            return drawable;
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
              final List<TimeSeriesDataItem> items =
                  dataset.getSeries(0).getItems();
              for (TimeSeriesDataItem i : items) {
                csv.write(
                  yearOnly.format(i.getPeriod().getStart()),
                  i.getValue().intValue()+"");
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
      } else {
        return null;
      }
    }

}
