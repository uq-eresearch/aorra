package charts.builder.spreadsheet;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.jfree.Attribute;
import charts.jfree.AttributedDataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class ChartConfigurator {

  private static final int SEARCH_DEPTH = 100;

  private final Map<Attribute, Object> defaults;

  private final SpreadsheetDataSource ds;

  private final StrSubstitutor substitutor;

  public ChartConfigurator(Map<Attribute, Object> defaults, 
      SpreadsheetDataSource ds, StrSubstitutor subst) {
    this.defaults = defaults;
    this.ds = ds;
    this.substitutor = subst;
  }

  private Pair<Integer, Integer> search(SpreadsheetDataSource ds, ChartType type) {
    for(int i=0;i<SEARCH_DEPTH;i++) {
      for(Pair<Integer, Integer> p : ImmutableList.of(Pair.of(i,0), Pair.of(0, i), Pair.of(1, i))) {
        if(isChartConfiguration(ds, p.getLeft(), p.getRight(), type)) {
          return p;
        }
      }
    }
    return null;
  }

  private boolean isChartConfiguration(SpreadsheetDataSource ds, int row, int col, ChartType type) {
    try {
      return isChartConfiguration(ds, row, col) &&
          ((type == null) ||
              readConfiguration(row, col).get(Attribute.TYPE) == type);
    } catch(MissingDataException e) {
      return false;
    }
  }

  private boolean isChartConfiguration(SpreadsheetDataSource ds, int row, int col) {
    try {
    return (StringUtils.equalsIgnoreCase("type", ds.select(row, col).asString()) &&
        (ChartType.lookup(ds.select(row, col+1).asString()) != null));
      } catch(MissingDataException e) {
      return false;
    }
  }

  public void configure(AttributedDataset dataset) {
    configure(dataset, null);
  }

  public void configure(AttributedDataset dataset, ChartType type) {
    if(defaults != null) {
      for(Map.Entry<Attribute, Object> me : defaults.entrySet()) {
        dataset.add(me.getKey(), substitute(me.getValue()));
      }
    }
    Map<Attribute, Object> cfg = getConfiguration(type);
    if(cfg != null) {
      for(Map.Entry<Attribute, Object> me : cfg.entrySet()) {
        dataset.add(me.getKey(),  substitute(me.getValue()));
      }
    }
  }

  private Object substitute(Object o) {
    if((o instanceof String) && (substitutor != null)) {
      return substitutor.replace(o);
    } else {
      return o;
    }
  }

  public Map<Attribute, Object> getConfiguration(ChartType type) {
    try {
      Pair<Integer, Integer> p = search(ds, type);
      if(p != null) {
        return readConfiguration(p.getLeft(), p.getRight());
      }
    } catch(MissingDataException e) {}
    return null;
  }

  private Map<Attribute, Object> readConfiguration(int row,
      int column) throws MissingDataException {
    Map<Attribute, Object> configuration = Maps.newHashMap();
    for(int r = row;true;r++) {
      if((r-row) >= 100) {
        throw new RuntimeException("expected less than 100 chart config entries");
      }
      String key = ds.select(r, column).asString();
      if(StringUtils.isBlank(key)) {
        break;
      }
      Attribute attribute = Attribute.lookup(key);
      if(attribute == null) {
        continue;
      }
      if(attribute.getType() == String.class) {
        String value = ds.select(r, column+1).asString();
        configuration.put(attribute, value);
      } else if(attribute.getName().equals("type")) {
        String value = ds.select(r, column+1).asString();
        ChartType type = ChartType.lookup(value);
        configuration.put(attribute, type);
      }
    }
    return configuration;
  }

}
