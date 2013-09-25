package charts.builder.spreadsheet;

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

  public abstract Chart build(SpreadsheetDataSource datasource, ChartType type,
      Region region, Dimension queryDimensions);

  public Chart build(SpreadsheetDataSource datasource, ChartType type,
          Region region, Dimension queryDimensions, Map<String, String> parameters) {
      return build(datasource, type, region, queryDimensions);
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

  public Map<String, List<String>> getParameters(List<DataSource> datasources, ChartType type) {
      Map<String, List<String>> result = Maps.newHashMap();
      for(DataSource ds : datasources) {
          result.putAll(getParameters((SpreadsheetDataSource)ds, type));
      }
      return result;
  }

  protected List<Chart> buildAllWithParameters(SpreadsheetDataSource datasource,
          ChartType type, Region region, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    if (type == null) {
        for (ChartType t : types) {
          Chart chart = build(datasource, t, region, queryDimensions, parameters);
          if (chart != null) {
            charts.add(chart);
          }
        }
      } else {
        Chart chart = build(datasource, type, region, queryDimensions, parameters);
        if (chart != null) {
          charts.add(chart);
        }
      }
      return charts;
  }

  private static boolean isSolution(Map<String, List<String>> parameters) {
      for(Map.Entry<String, List<String>> me : parameters.entrySet()) {
          if(me.getValue().size() > 1) {
              return false;
          }
      }
      return true;
  }

  private static Map<String, String> getSolution(Map<String, List<String>> parameters) {
      Map<String, String> result = Maps.newHashMap();
      for(Map.Entry<String, List<String>> me : parameters.entrySet()) {
          result.put(me.getKey(), me.getValue().get(0));
      }
      return result;
  }

  private static void permutate(List<Map<String, String>> result, Map<String, List<String>> parameters) {
      if(isSolution(parameters)) {
          result.add(getSolution(parameters));
      } else {
          for(Map.Entry<String, List<String>> me : parameters.entrySet()) {
              if(me.getValue().size() > 1) {
                  for(String s : me.getValue()) {
                      Map<String, List<String>> map = Maps.newHashMap(parameters);
                      map.put(me.getKey(), Lists.newArrayList(s));
                      permutate(result, map);
                  }
                  break;
              }
          }
      }
  }

  private static List<Map<String, String>> permutate(Map<String, List<String>> parameters) {
      List<Map<String, String>> result = Lists.newArrayList();
      permutate(result, parameters);
      return result;
  }

  protected List<Chart> buildAll(SpreadsheetDataSource datasource,
      ChartType type, Region region, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    Map<String, List<String>> supportedParameters = getParameters(datasource, type);
    Map<String, List<String>> preparedParameters = Maps.newHashMap();
    for(String key : supportedParameters.keySet()) {
        if((parameters != null) && parameters.containsKey(key)) {
            preparedParameters.put(key, Lists.newArrayList(parameters.get(key)));
        } else {
            preparedParameters.put(key, supportedParameters.get(key));
        }
    }
    for(Map<String, String> p : permutate(preparedParameters)) {
        charts.addAll(buildAllWithParameters(datasource, type, region, queryDimensions, p));
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
      List<Region> regions, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    for (DataSource datasource : datasources) {
      if (datasource instanceof SpreadsheetDataSource) {
        if (canHandle((SpreadsheetDataSource) datasource)) {
          charts.addAll(build((SpreadsheetDataSource) datasource, type,
              regions, queryDimensions, parameters));
        }
      }
    }
    return charts;
  }

  protected List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    if (!regions.isEmpty()) {
      for (Region region : regions) {
        charts.addAll(buildAll(datasource, type, region, queryDimensions, parameters));
      }
    } else {
      for (Region region : Region.values()) {
        charts.addAll(buildAll(datasource, type, region, queryDimensions, parameters));
      }
    }
    return charts;
  }

}
