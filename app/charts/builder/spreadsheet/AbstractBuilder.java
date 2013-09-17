package charts.builder.spreadsheet;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.getFirst;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pegdown.PegDownProcessor;

import charts.Chart;
import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartTypeBuilder;
import charts.builder.DataSource;
import charts.builder.DataSource.MissingDataException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public abstract class AbstractBuilder implements ChartTypeBuilder {

  private final List<ChartType> types;

  public AbstractBuilder(ChartType type) {
    types = Lists.newArrayList(type);
  }

  public AbstractBuilder(List<ChartType> types) {
    this.types = types;
  }

  public abstract boolean canHandle(SpreadsheetDataSource datasource);

  public abstract Chart build(SpreadsheetDataSource datasource, ChartType type,
      Region region, Dimension queryDimensions);

  protected String getCommentary(SpreadsheetDataSource datasource,
      Region region) throws UnsupportedFormatException {
    try {
      for (int nRow = 0; nRow < Integer.MAX_VALUE; nRow++) {
        final String k = datasource.select("Commentary", nRow, 0)
            .asString();
        final String v = datasource.select("Commentary", nRow, 1)
            .asString();
        if (k == null || v == null)
          break;
        if (region.getName().equals(k)) {
          return (new PegDownProcessor()).markdownToHtml(v);
        }
      }
    } catch (MissingDataException e) {}
    throw new UnsupportedFormatException();
  }

  public List<Chart> buildAll(SpreadsheetDataSource datasource,
      ChartType type, Region region, Dimension queryDimensions) {
    List<Chart> charts = Lists.newArrayList();
    if (type == null) {
      for (ChartType t : types) {
        Chart chart = build(datasource, t, region, queryDimensions);
        if (chart != null) {
          charts.add(chart);
        }
      }
    } else {
      Chart chart = build(datasource, type, region, queryDimensions);
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  @Override
  public boolean canHandle(ChartType type, List<DataSource> datasources) {
    if (type == null || types.contains(type)) {
      for (DataSource ds : datasources) {
        if ((ds instanceof SpreadsheetDataSource)
            && canHandle((SpreadsheetDataSource) ds)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public List<Chart> build(List<DataSource> datasources, ChartType type,
      List<Region> regions, Dimension queryDimensions) {
    List<Chart> charts = Lists.newArrayList();
    for (DataSource datasource : datasources) {
      if (datasource instanceof SpreadsheetDataSource) {
        if (canHandle((SpreadsheetDataSource) datasource)) {
          charts.addAll(build((SpreadsheetDataSource) datasource, type,
              regions, queryDimensions));
        }
      }
    }
    return charts;
  }

  protected List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Dimension queryDimensions) {
    List<Chart> charts = Lists.newArrayList();
    if (!regions.isEmpty()) {
      for (Region region : regions) {
        charts.addAll(buildAll(datasource, type, region, queryDimensions));
      }
    } else {
      for (Region region : Region.values()) {
        charts.addAll(buildAll(datasource, type, region, queryDimensions));
      }
    }
    return charts;
  }

  public static List<Region> getRegions(Map<String, String[]> query) {
    return Lists.newArrayList(Region.getRegions(getValues(query, "region")));
  }

  protected Dimension getChartSize(Map<String, String[]> query, int width, int height) {
    return new Dimension(getParam(query, "chartwidth", width),
            getParam(query, "chartheight", height));
  }

  protected int getParam(Map<String, String[]> query, String name, int def) {
    try {
      // Note: In Java, empty string will not parse to 0 - it's an error
      return Integer.parseInt(getFirst(getValues(query, name), ""));
    } catch(Exception e) {
      return def;
    }
  }

  protected static Set<String> getValues(Map<String, String[]> m, String key) {
    return ImmutableSet.copyOf(firstNonNull(m.get(key), new String[0]));
  }

  public static Dimension getQueryDimensions(Map<String, String[]> query) {
    final Dimension queryDimensions = new Dimension(750, 500);
    try {
      queryDimensions.setSize(
          Double.parseDouble(getFirst(getValues(query, "height"),"")),
          queryDimensions.getHeight());
    } catch (Exception e) {}
    try {
      queryDimensions.setSize(
          Double.parseDouble(getFirst(getValues(query, "width"),"")),
          queryDimensions.getWidth());
    } catch (Exception e) {}
    return queryDimensions;
  }

}
