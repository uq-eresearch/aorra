package charts.builder.spreadsheet;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.pegdown.PegDownProcessor;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.TrackingTowardsTargets;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TrackingTowardsTagetsBuilder extends AbstractBuilder {

  private static enum Series {
    CANE("Cane"), HORTICULTURE("Horticulture"), GRAZING("Grazing"), SEDIMENT(
        "Sediment"), TOTAL_NITROGEN("Total nitrogen"), PESTICIDES("Pesticides");

    private String name;

    private Series(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static final ImmutableMap<Series, Integer> ROW =
    new ImmutableMap.Builder<Series, Integer>()
      .put(Series.CANE, 1)
      .put(Series.HORTICULTURE, 2)
      .put(Series.GRAZING, 3)
      .put(Series.SEDIMENT, 4)
      .put(Series.TOTAL_NITROGEN, 5)
      .put(Series.PESTICIDES, 6)
      .build();

  public TrackingTowardsTagetsBuilder() {
    super(Lists.newArrayList(ChartType.TTT_CANE_AND_HORT,
        ChartType.TTT_GRAZING, ChartType.TTT_NITRO_AND_PEST,
        ChartType.TTT_SEDIMENT));
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      return "tracking towards tagets".equalsIgnoreCase(datasource.select("A1")
          .getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public Chart build(final SpreadsheetDataSource ds, final ChartType type,
      final Region region, final Map<String, String[]> query) {
    if (region == Region.GBR) {
      final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
      final double target;
      final String targetBy;
      try {
        switch (type) {
        case TTT_CANE_AND_HORT:
          addSeries(ds, dataset, Series.CANE);
          addSeries(ds, dataset, Series.HORTICULTURE);
          target = getTarget(ds, Series.CANE);
          targetBy = getTargetBy(ds, Series.CANE);
          break;
        case TTT_GRAZING:
          addSeries(ds, dataset, Series.GRAZING);
          target = getTarget(ds, Series.GRAZING);
          targetBy = getTargetBy(ds, Series.GRAZING);
          break;
        case TTT_NITRO_AND_PEST:
          addSeries(ds, dataset, Series.TOTAL_NITROGEN);
          addSeries(ds, dataset, Series.PESTICIDES);
          target = getTarget(ds, Series.TOTAL_NITROGEN);
          targetBy = getTargetBy(ds, Series.TOTAL_NITROGEN);
          break;
        case TTT_SEDIMENT:
          addSeries(ds, dataset, Series.SEDIMENT);
          target = getTarget(ds, Series.SEDIMENT);
          targetBy = getTargetBy(ds, Series.SEDIMENT);
          break;
        default:
          throw new RuntimeException("chart type not supported "
              + type.toString());
        }
      } catch (MissingDataException e) {
        throw new RuntimeException(e);
      }
      return new AbstractChart(query) {

        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          return new TrackingTowardsTargets().createChart(type, target,
              targetBy, dataset, getChartSize(query, 750, 500));
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          throw new Chart.UnsupportedFormatException();
        }

        @Override
        public String getCommentary() throws UnsupportedFormatException {
          final List<ChartType> cIdx = ImmutableList.copyOf(new ChartType[] {
            ChartType.TTT_CANE_AND_HORT,
            ChartType.TTT_GRAZING,
            ChartType.TTT_NITRO_AND_PEST,
            ChartType.TTT_SEDIMENT
          });
          try {
            for (int nRow = 0; nRow < Integer.MAX_VALUE; nRow++) {
              final String k = ds.select("Commentary", nRow, 0)
                  .asString();
              final String v = ds.select("Commentary", nRow,
                  cIdx.indexOf(type) + 1).asString();
              if (k == null || v == null)
                break;
              if (region.getName().equals(k)) {
                return (new PegDownProcessor()).markdownToHtml(v);
              }
            }
          } catch (MissingDataException e) {}
          throw new UnsupportedFormatException();
        }
      };
    }
    return null;
  }

  private void addSeries(SpreadsheetDataSource ds,
      DefaultCategoryDataset dataset, Series series) throws MissingDataException {
    Integer row = ROW.get(series);
    if (row == null) {
      throw new RuntimeException("no row configured for series " + series);
    }
    List<String> columns = getColumns(ds);
    for (int col = 0; col < columns.size(); col++) {
      String s = ds.select(row, col + 3).asString();
      try {
        Double value = new Double(s);
        dataset.addValue(value, series.toString(), columns.get(col));
      } catch (Exception e) {
        dataset.addValue(null, series.toString(), columns.get(col));
      }
    }

  }

  private List<String> getColumns(SpreadsheetDataSource ds) throws MissingDataException {
    List<String> columns = Lists.newArrayList();
    for (int col = 3; true; col++) {
      String s = ds.select(0, col).asString();
      if (StringUtils.isBlank(s)) {
        break;
      }
      columns.add(s);
    }
    return columns;
  }

  private double getTarget(SpreadsheetDataSource ds, Series series) throws MissingDataException {
    return ds.select(ROW.get(series), 1).asDouble();
  }

  private String getTargetBy(SpreadsheetDataSource ds, Series series) throws MissingDataException {
    return ds.select(ROW.get(series), 2).asInteger().toString();
  }

}
