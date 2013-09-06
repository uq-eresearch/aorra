package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.DefaultCategoryDataset;
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

  private DefaultCategoryDataset createDataset(
      SpreadsheetDataSource datasource, Region region) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    final String series = "rainfall";
    Integer row = ROW.get(region);
    try {
      for (int i = 1; true; i++) {
        String year = datasource.select(0, i).asString();
        if (StringUtils.equalsIgnoreCase("Annual Average", year)) {
          break;
        }
        String outbreaks = datasource.select(row, i).asString();
        if (StringUtils.isBlank(year) || StringUtils.isBlank(outbreaks)) {
          break;
        }
        double val = Double.parseDouble(outbreaks);
        dataset.addValue(val, series, Integer.toString(parseYear(year)));
      }
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
      final Region region, final Map<String, String[]> query) {
    if (!ROW.containsKey(region)) {
      return null;
    }
    final DefaultCategoryDataset dataset = createDataset(datasource, region);
    final AbstractBuilder thisBuilder = this;
    final Chart chart = new AbstractChart(query) {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(ChartType.ANNUAL_RAINFALL, region);
      }

      @Override
      public Drawable getChart() {
        return new AnnualRainfall().createChart(region.getName(), dataset,
            getChartSize(query, 750, 500));
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

      @Override
      public String getCommentary() throws UnsupportedFormatException {
        return thisBuilder.getCommentary(datasource, region);
      }
    };
    return chart;
  }

}
