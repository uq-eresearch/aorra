package charts.builder.spreadsheet;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import charts.Chart;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartTypeBuilder;
import charts.builder.DataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class AbstractBuilder implements ChartTypeBuilder {

  private final List<ChartType> types;

  public AbstractBuilder(ChartType type) {
    types = Lists.newArrayList(type);
  }

  public AbstractBuilder(List<ChartType> types) {
    this.types = types;
  }

  protected abstract boolean canHandle(SpreadsheetDataSource datasource);

  protected Chart build(SpreadsheetDataSource datasource, ChartType type,
      Region region) {
      throw new RuntimeException("override me");
  }

  protected Chart build(SpreadsheetDataSource datasource, ChartType type,
          Region region, Map<String, String> parameters) {
      if(parameters.isEmpty()) {
          return build(datasource, type, region);
      } else {
          throw new RuntimeException("override me");
      }
  }

  protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
      return Maps.newHashMap();
  }

  protected Map<String, List<String>> getParameters(DataSource datasource, ChartType type) {
    return getParameters((SpreadsheetDataSource)datasource, type);
  }

  @Override
  public List<Chart> build(DataSource datasource, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    if((type == null) || types.contains(type)) {
      if (datasource instanceof SpreadsheetDataSource) {
        for(int i = 0; i<((SpreadsheetDataSource)datasource).sheets();i++) {
          SpreadsheetDataSource ds = ((SpreadsheetDataSource)datasource).toSheet(i);
          if(canHandle(ds)) {
            charts.addAll(build(ds, type, regions, parameters));
          }
        }
      }
    }
    return charts;
  }

  private List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    checkNotNull(regions, "Regions can be empty, but not null.");
    Map<String, List<String>> m = Maps.newHashMap();
    Map<String, List<String>> supportedParameters = getParameters(datasource, type);
    for(String key : supportedParameters.keySet()) {
        if ((parameters != null) && parameters.containsKey(key)) {
            m.put(key, asList(parameters.get(key)));
        } else {
            m.put(key, supportedParameters.get(key));
        }
    }
    final List<ChartType> t =
        type == null ? types : asList(type);
    final List<Region> r =
        regions.isEmpty() ? asList(Region.values()) : regions;
    final List<Chart> charts = Lists.newLinkedList();
    for (Object o : ChartPermutations.apply(t, r, m)) {
      ChartPermutation p = (ChartPermutation)o;
      final Chart chart = build(datasource, p.chartType(), p.region(), p.javaParams());
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

}
