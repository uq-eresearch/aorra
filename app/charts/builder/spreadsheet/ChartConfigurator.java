package charts.builder.spreadsheet;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.util.CellReference;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.graphics.Colors;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;
import charts.jfree.AttributedDataset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ChartConfigurator {

  private static final int SEARCH_DEPTH = 100;

  private static class Configuration {

    private final String types;
    private final AttributeMap attributes;

    public Configuration(String types, AttributeMap attributes) {
      this.types = types;
      this.attributes = attributes;
    }

    public boolean isFor(ChartType type) {
      if(StringUtils.isBlank(types) || (type == null) || StringUtils.equals("*", types)) {
        return true;
      }
      String[] types = StringUtils.split(this.types, ',');
      for(String s : types) {
        if(type==ChartType.lookup(StringUtils.strip(s))) {
          return true;
        }
      }
      return false;
    }

    public AttributeMap attributes() {
      return attributes;
    }
  }

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
      return isChartConfiguration(ds, row, col) && readConfiguration(row, col).isFor(type);
    } catch(MissingDataException e) {
      return false;
    }
  }

  private boolean isChartConfiguration(SpreadsheetDataSource ds, int row, int col) {
    try {
      return isChartConfiguration(ds.select(row, col).asString());
    } catch(MissingDataException e) {
      return false;
    }
  }

  private boolean isChartConfiguration(String key) {
    return (StringUtils.equalsIgnoreCase("type", key) ||
        StringUtils.equalsIgnoreCase("chart configuration", key));
  }

  public void configure(AttributedDataset dataset, ChartType type) {
    dataset.attrMap().putAll(substitute(defaults));
    Configuration cfg = getConfiguration(type);
    if(cfg != null) {
      dataset.attrMap().putAll(substitute(cfg.attributes()));
    }
  }

  private AttributeMap substitute(AttributeMap m) {
    if((substitutor != null) && (m != null)) {
      for(Attribute<String> a : m.getKeys(String.class)) {
        m.put(a, substitutor.replace(m.get(a)));
      }
    }
    return m;
  }

  public Configuration getConfiguration(ChartType type) {
    try {
      Pair<Integer, Integer> p = search(ds, type);
      if(p != null) {
        return readConfiguration(p.getLeft(), p.getRight());
      }
    } catch(MissingDataException e) {}
    return null;
  }

  @SuppressWarnings("unchecked")
  private Configuration readConfiguration(int row,
      int column) throws MissingDataException {
    String types = null;
    AttributeMap.Builder mb = new AttributeMap.Builder();
    for(int r = row;true;r++) {
      if((r-row) >= 100) {
        throw new RuntimeException("expected less than 100 chart config entries");
      }
      String key = ds.select(r, column).asString();
      if(StringUtils.isBlank(key)) {
        break;
      }
      Value val = ds.select(r, column+1);
      if(isChartConfiguration(key)) {
        if(types != null) {
          break;
        } else {
          types = val.asString();
        }
      } else {
        Attribute<?> attribute = Attribute.lookup(key);
        if(attribute == null) {
          continue;
        }
        if(attribute.getType() == String.class) {
          mb.put((Attribute<String>)attribute, val.asString());
        } else if(attribute.getType().equals(Color.class)) {
          mb.put((Attribute<Color>)attribute, val.asColor());
        } else if(attribute.getType().equals(Color[].class)) {
          mb.put((Attribute<Color[]>)attribute, getColors(val.asString()));
        }
      }
    }
    return new Configuration(types, mb.build());
  }

  private Color[] getColors(String s) {
    if(StringUtils.startsWith(StringUtils.strip(s), "#")) {
      return fromHex(s);
    } else if(StringUtils.startsWithIgnoreCase(StringUtils.strip(s), "from")) {
      return fromCells(s);
    } else {
      return new Color[0];
    }
  }

  private Color[] fromHex(String s) {
    List<Color> colors = Lists.newArrayList();
    for(String color : StringUtils.split(s, ',')) {
      Color c = Colors.fromHex(StringUtils.strip(color));
      if(c != null) {
        colors.add(c);
      }
    }
    return colors.toArray(new Color[colors.size()]);
  }

  private Color[] fromCells(String s) {
    List<Color> colors = Lists.newArrayList();
    Pattern p = Pattern.compile("from.*([a-z]+)([0-9]+).*([a-z]+)([0-9]+)", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(s);
    if(m.matches()) {
      String c1 = m.group(1);
      String r1 = m.group(2);
      String c2 = m.group(3);
      String r2 = m.group(4);
      CellReference cr1 = new CellReference(c1+r1);
      CellReference cr2 = new CellReference(c2+r2);
      if(cr1.getRow() == cr2.getRow()) {
        if(Math.abs(cr1.getCol()-cr2.getCol()) < 100) {
          for(int col = cr1.getCol();true;col += (cr1.getCol()<cr2.getCol()?1:-1)) {
            try {
              Color c = ds.select(cr1.getRow(), col).asColor();
              if(c!=null) {
                colors.add(c);
              }
            } catch(MissingDataException e) {}
            if(col == cr2.getCol()) {
              break;
            }
          }
        }
      } else if(cr1.getCol() == cr2.getCol()) {
        if(Math.abs(cr1.getRow()-cr2.getRow()) < 100) {
          for(int row = cr1.getRow();true;row += (cr1.getRow()<cr2.getRow()?1:-1)) {
            try {
              Color c = ds.select(row, cr1.getCol()).asColor();
              if(c!=null) {
                colors.add(c);
              }
            } catch(MissingDataException e) {}
            if(row == cr2.getRow()) {
              break;
            }
          }
        }
      }
    }
    return colors.toArray(new Color[colors.size()]);
  }
}
