package charts.builder.spreadsheet;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
  private final int row;
  private final int column;

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

  public void configure(ADCDataset dataset) {
    configure(dataset, null);
  }

  public void configure(ADCDataset dataset, ChartType type) {
    Map<Attribute, Object> cfg = getConfiguration(type);
    if(defaults != null) {
      for(Map.Entry<Attribute, Object> me : defaults.entrySet()) {
        dataset.add(me.getKey(), me.getValue());
      }
    }
    for(Map.Entry<Attribute, Object> me : cfg.entrySet()) {
      dataset.add(me.getKey(), me.getValue());
    }
  }

  public Map<Attribute, Object> getConfiguration(ChartType type) {
    try {
      int col = column;
      while(true) {
        Map<Attribute, Object> cfg = readConfiguration(row, col);
        if(type == null || 
            (cfg.containsKey(Attribute.TYPE) && (type == cfg.get(Attribute.TYPE)))) {
          return cfg;
        } else {
          col+=2;
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
