package charts.builder.spreadsheet;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.Maps;

public class ChartConfigurator {

  private int maxRow = 5000;

  private final Map<Attribute, Object> defaults;

  private final SpreadsheetDataSource ds;

  //start of chart configuration block
  private final Integer row;
  private final Integer column;

  public ChartConfigurator(Map<Attribute, Object> defaults, 
      SpreadsheetDataSource ds, int row, int column) {
    this.defaults = defaults;
    this.ds = ds;
    this.row = row;
    this.column = column;
  }

  public ChartConfigurator(SpreadsheetDataSource ds, int row, int column) {
    this(null, ds, row, column);
  }

  public ChartConfigurator(SpreadsheetDataSource ds) {
    this(null, ds);
  }

  public ChartConfigurator(Map<Attribute, Object> defaults, 
      SpreadsheetDataSource ds) {
    this.defaults = defaults;
    this.ds = ds;
    this.row = null;
    this.column = null;
  }

  private Pair<Integer, Integer> search(SpreadsheetDataSource ds) {
    for(int row = 0;row<100;row++) {
      try {
        if(StringUtils.equalsIgnoreCase("type", ds.select(row, 0).asString()) &&
            (ChartType.lookup(ds.select(row, 1).asString()) != null)) {
          return Pair.of(row, 0);
        }
      } catch(MissingDataException e) {}
    }
    return null;
  }

  public void configure(ADCDataset dataset) {
    configure(dataset, null);
  }

  public void configure(ADCDataset dataset, ChartType type) {
    if(defaults != null) {
      for(Map.Entry<Attribute, Object> me : defaults.entrySet()) {
        dataset.add(me.getKey(), me.getValue());
      }
    }
    Map<Attribute, Object> cfg = getConfiguration(type);
    if(cfg != null) {
      for(Map.Entry<Attribute, Object> me : cfg.entrySet()) {
        dataset.add(me.getKey(), me.getValue());
      }
    }
  }

  public Map<Attribute, Object> getConfiguration(ChartType type) {
    try {
      int c;
      int r;
      if(column == null) {
        Pair<Integer, Integer> p = search(ds);
        if(p == null) {
          return null;
        } else {
          r = p.getLeft();
          c = p.getRight();
        }
      } else {
        c = column;
        r = row;
      }
      while(true) {
        Map<Attribute, Object> cfg = readConfiguration(r, c);
        if(type == null || 
            (!cfg.containsKey(Attribute.TYPE)) ||
            (cfg.containsKey(Attribute.TYPE) && (type == cfg.get(Attribute.TYPE)))) {
          return cfg;
        } else {
          c+=2;
        }
      }
    } catch(MissingDataException e) {
      return Maps.newHashMap();
    }
  }

  private Map<Attribute, Object> readConfiguration(int row,
      int column) throws MissingDataException {
    Map<Attribute, Object> configuration = Maps.newHashMap();
    for(int r = row;r<maxRow;r++) {
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
