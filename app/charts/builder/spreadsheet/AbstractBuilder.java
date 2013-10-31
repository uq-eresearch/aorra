package charts.builder.spreadsheet;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.pegdown.PegDownProcessor;

import charts.Chart;
import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartTypeBuilder;
import charts.builder.DataSource;
import charts.builder.DataSource.MissingDataException;

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

  public abstract boolean canHandle(SpreadsheetDataSource datasource);

  public Chart build(SpreadsheetDataSource datasource, ChartType type,
      Region region, Dimension queryDimensions) {
      throw new RuntimeException("override me");
  }

  public Chart build(SpreadsheetDataSource datasource, ChartType type,
          Region region, Dimension queryDimensions, Map<String, String> parameters) {
      if(parameters.isEmpty()) {
          return build(datasource, type, region, queryDimensions);
      } else {
          throw new RuntimeException("override me");
      }
  }

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

  protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
      return Maps.newHashMap();
  }

  @Override
  public Map<String, List<String>> getParameters(List<DataSource> datasources, ChartType type) {
      Map<String, List<String>> result = Maps.newHashMap();
      for(DataSource ds : datasources) {
          if(setupDataSource((SpreadsheetDataSource)ds)) {
              result.putAll(getParameters((SpreadsheetDataSource)ds, type));
          }
      }
      return result;
  }

  @Override
  public boolean canHandle(ChartType type, List<DataSource> datasources) {
    if (type == null || types.contains(type)) {
      for (DataSource ds : datasources) {
        if ((ds instanceof SpreadsheetDataSource)
            && setupDataSource((SpreadsheetDataSource) ds)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public List<Chart> build(List<DataSource> datasources, ChartType type,
      List<Region> regions, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    for (DataSource datasource : datasources) {
      if (datasource instanceof SpreadsheetDataSource) {
        if (setupDataSource((SpreadsheetDataSource) datasource)) {
          charts.addAll(build((SpreadsheetDataSource) datasource, type,
              regions, queryDimensions, parameters));
        }
      }
    }
    return charts;
  }

  private List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Dimension queryDimensions, Map<String, String> parameters) {
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
    for (ChartPermutation p : ChartPermutations.apply(t, r, m)) {
      final Chart chart = build(datasource, p.chartType(), p.region(),
          queryDimensions, p.javaParams());
      if (chart != null) {
        charts.add(chart);
      }
    }
    return charts;
  }

  private boolean setupDataSource(SpreadsheetDataSource datasource) {
      for(int i=0;i<datasource.sheets();i++) {
          datasource.setDefaultSheet(i);
          if(canHandle(datasource)) {
              return true;
          }
      }
      return false;
  }

}
