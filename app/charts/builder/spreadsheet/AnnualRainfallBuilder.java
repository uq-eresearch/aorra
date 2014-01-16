package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.AnnualRainfall;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableMap;

public class AnnualRainfallBuilder extends AbstractBuilder {

  private static final ImmutableMap<Region, Integer> ROW =
      new ImmutableMap.Builder<Region, Integer>()
        .put(Region.BURDEKIN, 1)
        .put(Region.FITZROY, 2)
        .put(Region.MACKAY_WHITSUNDAY, 3)
        .put(Region.BURNETT_MARY, 4)
        .put(Region.WET_TROPICS, 5)
        .put(Region.GBR, 6)
        .build();

  public AnnualRainfallBuilder() {
    super(ChartType.ANNUAL_RAINFALL);
  }

  private ADCDataset createDataset(
      SpreadsheetDataSource datasource, Region region) {
    ADCDataset dataset = new ADCDataset();
    final String series = "rainfall";
    Integer row = ROW.get(region);
    try {
      for (int i = 1; true; i++) {
        String year = datasource.select(0, i).asString();
        if (StringUtils.equalsIgnoreCase("Annual Average", year)) {
          dataset.add(Attribute.TITLE, datasource.select(row,i+1).asString());
          break;
        }
        String rainfall = datasource.select(row, i).asString();
        if (StringUtils.isBlank(year) || StringUtils.isBlank(rainfall)) {
          break;
        }
        double val = Double.parseDouble(rainfall);
        dataset.addValue(val, series, Integer.toString(parseYear(year)));
      }
      dataset.add(Attribute.SERIES_COLOR, datasource.select(row, 0).asColor());
    } catch (Exception e) {
      e.printStackTrace();
    }
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
    try {
      return "Great Barrier Reef".equalsIgnoreCase(datasource.select("A7")
          .getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public Chart build(final SpreadsheetDataSource datasource, ChartType type,
      final Region region) {
    if (!ROW.containsKey(region)) {
      return null;
    }
    final ADCDataset dataset = createDataset(datasource, region);
    dataset.add(Attribute.REGION, region);
    final Chart chart = new AbstractChart() {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(ChartType.ANNUAL_RAINFALL, region);
      }

      @Override
      public Drawable getChart() {
        return new AnnualRainfall().createChart(dataset, new Dimension(750, 500));
      }

      @Override
      public String getCSV() {
        final StringWriter sw = new StringWriter();
        try {
          final CsvListWriter csv = new CsvListWriter(sw,
              CsvPreference.STANDARD_PREFERENCE);
          csv.write("Year", "Railfall (mm)");
          for (int i = 0; i < dataset.getColumnCount(); i++) {
            csv.write(dataset.getColumnKey(i), dataset.getValue(0, i));
          }
          csv.close();
        } catch (IOException e) {
          // How on earth would IOException occur with a StringWriter?
          throw new RuntimeException(e);
        }
        return sw.toString();
      }
    };
    return chart;
  }

}
