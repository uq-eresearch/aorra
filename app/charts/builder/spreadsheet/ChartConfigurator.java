package charts.builder.spreadsheet;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import charts.builder.DataSource.MissingDataException;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class ChartConfigurator {

  private int maxRow = 5000;

  private Map<Attribute<String>, String> defaults;

  public ChartConfigurator(Map<Attribute<String>, String> defaults) {
    this.defaults = defaults;
  }

  @SuppressWarnings("unchecked")
  public void configure(ADCDataset dataset, SpreadsheetDataSource ds,
      int row, int column) {
    try {
      for(Map.Entry<Attribute<String>, String> me : defaults.entrySet()) {
        dataset.add(me.getKey(), me.getValue());
      }
      for(int r = row;r<maxRow;r++) {
        String key = ds.select(r, column).asString();
        if(StringUtils.isBlank(key)) {
          break;
        }
        Attribute<?> attribute = Attribute.lookup(key);
        if(attribute == null) {
          continue;
        }
        if(attribute.getType() == String.class) {
          String value = ds.select(r, column+1).asString();
          dataset.add((Attribute<String>)attribute, value);
        }
      }
    } catch(MissingDataException e) {
    }
  }

}
