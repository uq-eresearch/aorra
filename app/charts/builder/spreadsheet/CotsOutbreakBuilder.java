package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.Year;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.CotsOutbreak;
import charts.jfree.ATSCollection;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableMap;

public class CotsOutbreakBuilder extends AbstractBuilder {

  private static final String TITLE = "Crown-of-thorns starfish outbreaks";

  private static final Map<Attribute, Object> ccDefaults = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE, TITLE,
      Attribute.DOMAIN_AXIS_LABEL, "Year",
      Attribute.RANGE_AXIS_LABEL, "Outbreaks"
      );

  public CotsOutbreakBuilder() {
    super(ChartType.COTS_OUTBREAK);
  }

  private boolean isCotsOutbreakSpreadsheet(SpreadsheetDataSource datasource) {
    try {
      return "TOTALOUTBREAKS".equalsIgnoreCase(datasource.select("B1")
          .getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  private ATSCollection createDataset(SpreadsheetDataSource datasource) {
    TimeSeries s1 = new TimeSeries("outbreaks");
    try {
      for (int i = 2; true; i++) {
        String year = datasource.select("A" + i).getValue();
        String outbreaks = datasource.select("B" + i).getValue();
        if (StringUtils.isBlank(year) || StringUtils.isBlank(outbreaks))
          break;
        double val = Double.parseDouble(outbreaks);
        s1.add(new Year(parseYear(year)), val);
      }
    } catch (Exception e) {
    }
    ATSCollection dataset = new ATSCollection();
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
  public Chart build(final SpreadsheetDataSource datasource, ChartType type,
      final Region region) {
    if (!region.equals(Region.GBR)) {
      return null;
    }
    final ATSCollection dataset = createDataset(datasource);
    configurator(datasource, ccDefaults, type, region).configure(dataset);
    final Drawable d = new CotsOutbreak().createChart(dataset, new Dimension(750, 500));
    final Chart chart = new AbstractChart() {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(ChartType.COTS_OUTBREAK, region);
      }

      @Override
      public Drawable getChart() {
        return d;
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
