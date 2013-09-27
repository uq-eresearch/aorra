package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.util.Arrays;
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

  private static final String CHART_TYPE = "charttype";
  private static final String REGIONS = "regions";

  @SuppressWarnings("rawtypes")
  private static class Permutations {

      private boolean isSolution(Map<String, List> parameters) {
          for(Map.Entry<String, List> me : parameters.entrySet()) {
              if(me.getValue().size() > 1) {
                  return false;
              }
          }
          return true;
      }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getSolution(Map<String, List> map) {
          Map result = Maps.newHashMap();
          for(Map.Entry<String, List> me : map.entrySet()) {
              result.put(me.getKey(), me.getValue().get(0));
          }
          return result;
      }

      private void permutate(List<Map<String, ?>> result, Map<String, List> map) {
          if(isSolution(map)) {
              result.add(getSolution(map));
          } else {
              for(Map.Entry<String, List> me : map.entrySet()) {
                  if(me.getValue().size() > 1) {
                      for(Object o : me.getValue()) {
                          Map<String, List> m = Maps.newHashMap(map);
                          m.put(me.getKey(), Lists.newArrayList(o));
                          permutate(result, m);
                      }
                      break;
                  }
              }
          }
      }

      public List<Map<String, ?>> permutate(Map<String, List> map) {
          for(Map.Entry<String, List> me : map.entrySet()) {
              if(me.getValue().isEmpty()) {
                  throw new IllegalArgumentException(String.format(
                          "list for map entry %s is empty", me.getKey()));
              }
          }
          List<Map<String, ?>> result = Lists.newArrayList();
          permutate(result, map);
          return result;
      }
  }

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
          Region region, Dimension queryDimensions, Map<String, ?> parameters) {
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

  public Map<String, List<String>> getParameters(List<DataSource> datasources, ChartType type) {
      Map<String, List<String>> result = Maps.newHashMap();
      for(DataSource ds : datasources) {
          result.putAll(getParameters((SpreadsheetDataSource)ds, type));
      }
      return result;
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

  private List<Chart> build(SpreadsheetDataSource datasource, ChartType type,
      List<Region> regions, Dimension queryDimensions, Map<String, String> parameters) {
    List<Chart> charts = Lists.newArrayList();
    @SuppressWarnings("rawtypes")
    Map<String, List> m = Maps.newHashMap();
    Map<String, List<String>> supportedParameters = getParameters(datasource, type);
    for(String key : supportedParameters.keySet()) {
        if((parameters != null) && parameters.containsKey(key)) {
            m.put(key, Lists.newArrayList(parameters.get(key)));
        } else {
            m.put(key, supportedParameters.get(key));
        }
    }
    if(type == null) {
        m.put(CHART_TYPE, types);
    } else {
        m.put(CHART_TYPE, Arrays.asList(type));
    }
    if((regions == null) || regions.isEmpty()) {
        m.put(REGIONS, Arrays.asList(Region.values()));
    } else {
        m.put(REGIONS, regions);
    }
    for(Map<String, ?> p : new Permutations().permutate(m)) {
        ChartType t = (ChartType)p.remove(CHART_TYPE);
        Region r = (Region)p.remove(REGIONS);
        Chart chart = build(datasource, t, r, queryDimensions, p);
        if(chart != null) {
            charts.add(chart);
        }
    }
    return charts;
  }

}
