package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.pegdown.PegDownProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

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

public class TrackingTowardsTargetsBuilder extends AbstractBuilder {

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

  public TrackingTowardsTargetsBuilder() {
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
      final Region region, Dimension dimensions) {
    if (region == Region.GBR) {
      return new AbstractChart(dimensions) {

        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          try {
            final double target = getTarget(ds, getTargetSeries(type));
            final String targetBy = getTargetBy(ds, getTargetSeries(type));
            return new TrackingTowardsTargets().createChart(type, target,
                targetBy, createDataset(ds, type), new Dimension(750, 500));
          } catch (MissingDataException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          final StringWriter sw = new StringWriter();
          try {
            final CategoryDataset dataset =
                createDataset(ds, type);
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            @SuppressWarnings("unchecked")
            List<String> columnKeys = dataset.getColumnKeys();
            @SuppressWarnings("unchecked")
            List<String> rowKeys = dataset.getRowKeys();
            final List<String> heading = ImmutableList.<String>builder()
                .add(format("%s %s", region, type))
                .add(format("%% Target by " +
                    getTargetBy(ds, getTargetSeries(type))))
                .addAll(columnKeys)
                .build();
            csv.write(heading); // Heading, 2008-2009, 2009-2010
            final double target = getTarget(ds, getTargetSeries(type));
            for (String row : rowKeys) {
              List<String> line = newLinkedList();
              line.add(row);
              line.add(format("%.0f", target * 100));
              for (String col : columnKeys) {
                Number n = dataset.getValue(row, col);
                line.add(n == null ? "" : format("%.0f", n.doubleValue()*100));
              }
              csv.write(line);
            }
            csv.close();
          } catch (IOException e) {
            // How on earth would you get an IOException with a StringWriter?
            throw new RuntimeException(e);
          } catch (MissingDataException e) {
            throw new RuntimeException(e);
          }
          return sw.toString();
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

  protected CategoryDataset createDataset(SpreadsheetDataSource ds,
      ChartType type) {
    final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    try {
      switch (type) {
      case TTT_CANE_AND_HORT:
        addSeries(ds, dataset, Series.CANE);
        addSeries(ds, dataset, Series.HORTICULTURE);
        break;
      case TTT_GRAZING:
        addSeries(ds, dataset, Series.GRAZING);
        break;
      case TTT_NITRO_AND_PEST:
        addSeries(ds, dataset, Series.TOTAL_NITROGEN);
        addSeries(ds, dataset, Series.PESTICIDES);
        break;
      case TTT_SEDIMENT:
        addSeries(ds, dataset, Series.SEDIMENT);
        break;
        //$CASES-OMITTED$
      default:
        throw new RuntimeException("chart type not supported "
            + type.toString());
      }
    } catch (MissingDataException e) {
      throw new RuntimeException(e);
    }
    return dataset;
  }

  protected Series getTargetSeries(ChartType chartType) {
    switch (chartType) {
    case TTT_CANE_AND_HORT:
      return Series.CANE;
    case TTT_GRAZING:
      return Series.GRAZING;
    case TTT_NITRO_AND_PEST:
      return Series.TOTAL_NITROGEN;
    case TTT_SEDIMENT:
      return Series.SEDIMENT;
      //$CASES-OMITTED$
    default:
      throw new RuntimeException("chart type not supported "+chartType);
    }
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
