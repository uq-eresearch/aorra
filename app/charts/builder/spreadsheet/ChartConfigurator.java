package charts.builder.spreadsheet;

import java.awt.Color;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;
import charts.jfree.AttributedDataset;

import com.google.common.collect.ImmutableList;

public class ChartConfigurator {

  private static final int SEARCH_DEPTH = 100;

  private final AttributeMap defaults;

  private final SpreadsheetDataSource ds;

  private final StrSubstitutor substitutor;

  public ChartConfigurator(AttributeMap defaults, 
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
    dataset.attrMap().putAll(substitute(defaults));
    dataset.attrMap().putAll(substitute(getConfiguration(type)));
    
  }

  private AttributeMap substitute(AttributeMap m) {
    if((substitutor != null) && (m != null)) {
      for(Attribute<String> a : m.getKeys(String.class)) {
        m.put(a, substitutor.replace(m.get(a)));
      }
    }
    return m;
  }

  public AttributeMap getConfiguration(ChartType type) {
    try {
      Pair<Integer, Integer> p = search(ds, type);
      if(p != null) {
        return readConfiguration(p.getLeft(), p.getRight());
      }
    } catch(MissingDataException e) {}
    return null;
  }

  @SuppressWarnings("unchecked")
  private AttributeMap readConfiguration(int row,
      int column) throws MissingDataException {
    AttributeMap.Builder mb = new AttributeMap.Builder();
    for(int r = row;true;r++) {
      if((r-row) >= 100) {
        throw new RuntimeException("expected less than 100 chart config entries");
      }
      String key = ds.select(r, column).asString();
      if(StringUtils.isBlank(key)) {
        break;
      }
      Attribute<?> attribute = Attribute.lookup(key);
      if(attribute == null) {
        continue;
      }
      Value val = ds.select(r, column+1);
      if(attribute.getType() == String.class) {
        mb.put((Attribute<String>)attribute, val.asString());
      } else if(attribute.getName().equals("type")) {
        mb.put((Attribute<ChartType>)attribute, ChartType.lookup(val.asString()));
      } else if(attribute.getType().equals(Color.class)) {
        mb.put((Attribute<Color>)attribute, val.asColor());
      }
    }
    return mb.build();
  }

}
