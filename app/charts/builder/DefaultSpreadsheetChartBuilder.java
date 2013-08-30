package charts.builder;

import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
    List<Chart> charts = Lists.newArrayList();
    List<Region> regions = getRegion(query);
    if (regions == null || regions.isEmpty()) {
      regions = ImmutableList.copyOf(Region.values());
    }
    for (Region region : regions) {
      Chart chart = build(datasource, region, query);
      if(chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  protected List<Region> getRegion(Map<String, String[]> query) {
    final String[] regions = query.get("region");
    if (regions == null || regions.length == 0) {
      return Collections.emptyList();
    }
    return Lists.newArrayList(Region.getRegions(Sets.newHashSet(regions)));
  }

  Dimension getChartSize(Map<String, String[]> query, int width, int height) {
    return new Dimension(getParam(query, "chartwidth", width),
            getParam(query, "chartheight", height));
  }

  int getParam(Map<String, String[]> query, String name, int def) {
    try {
      String tmp = getParam(query, name);
      if (StringUtils.isBlank(tmp)) {
        throw new Exception("Param is blank.");
      }
      return Integer.parseInt(tmp);
    } catch(Exception e) {
      return def;
    }
  }

  String getParam(Map<String, String[]> query, String name) {
    final String[] tmp = query.get(name);
    if (tmp == null || tmp.length == 0) {
      return null;
    }
    return tmp[0];
  }

}
