package charts.builder;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.base.Objects.firstNonNull;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.Set;

import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public abstract class DefaultSpreadsheetChartBuilder implements ChartTypeBuilder {

  private final ChartType type;

  public DefaultSpreadsheetChartBuilder(ChartType type) {
    this.type = type;
  }

  abstract boolean canHandle(DataSource datasource);

  abstract Chart build(DataSource datasource, Region region,
      Map<String, String[]> query);

  @Override
  public boolean canHandle(ChartType type, List<DataSource> datasources) {
    if (type == null || type.equals(this.type)) {
      for (DataSource ds : datasources) {
        if (canHandle(ds)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public List<Chart> build(List<DataSource> datasources,
          Map<String, String[]> query) {
    List<Chart> charts = Lists.newArrayList();
    for (DataSource datasource : datasources) {
      if (canHandle(datasource)) {
        charts.addAll(build(datasource, query));
      }
    }
    return charts;
  }

  List<Chart> build(DataSource datasource, Map<String, String[]> query) {
    List<Region> regions = getRegion(query);
    if (regions.isEmpty()) {
      regions = ImmutableList.copyOf(Region.values());
    }
    final List<Chart> charts = Lists.newArrayList();
    for (Region region : regions) {
      Chart chart = build(datasource, region, query);
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  protected List<Region> getRegion(Map<String, String[]> query) {
    return Lists.newArrayList(Region.getRegions(getValues(query, "region")));
  }

  Dimension getChartSize(Map<String, String[]> query, int width, int height) {
    return new Dimension(getParam(query, "chartwidth", width),
            getParam(query, "chartheight", height));
  }

  int getParam(Map<String, String[]> query, String name, int def) {
    try {
      // Note: In Java, empty string will not parse to 0 - it's an error
      return Integer.parseInt(getFirst(getValues(query, name), ""));
    } catch(Exception e) {
      return def;
    }
  }

  private static Set<String> getValues(Map<String, String[]> m, String key) {
    return ImmutableSet.copyOf(firstNonNull(m.get(key), new String[0]));
  }

}
