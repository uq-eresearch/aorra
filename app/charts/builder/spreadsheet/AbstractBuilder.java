package charts.builder.spreadsheet;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.getFirst;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Set;

import charts.builder.Chart;
import charts.builder.ChartType;
import charts.builder.ChartTypeBuilder;
import charts.builder.DataSource;
import charts.builder.Region;

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
      Region region, Map<String, String[]> query);

  public List<Chart> buildAll(SpreadsheetDataSource datasource,
      ChartType type, Region region, Map<String, String[]> query) {
    List<Chart> charts = Lists.newArrayList();
    if (type == null) {
      for (ChartType t : types) {
        Chart chart = build(datasource, t, region, query);
        if (chart != null) {
          charts.add(chart);
        }
      }
    } else {
      Chart chart = build(datasource, type, region, query);
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
      Map<String, String[]> query) {
    List<Chart> charts = Lists.newArrayList();
    for (DataSource datasource : datasources) {
      if (datasource instanceof SpreadsheetDataSource) {
        if (canHandle((SpreadsheetDataSource) datasource)) {
          charts.addAll(build((SpreadsheetDataSource) datasource, type, query));
        }
      }
    }
    return charts;
  }

  protected List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      Map<String, String[]> query) {
    List<Chart> charts = Lists.newArrayList();
    List<Region> regions = getRegion(query);
    if (regions != null && !regions.isEmpty()) {
      for (Region region : regions) {
        charts.addAll(buildAll(datasource, type, region, query));
      }
    } else {
      for (Region region : Region.values()) {
        charts.addAll(buildAll(datasource, type, region, query));
      }
    }
    return charts;
  }

  protected List<Region> getRegion(Map<String, String[]> query) {
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

}
