package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import play.Logger;
import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.graphics.ProgressTable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ProgressTableBuilder extends AbstractBuilder {

  private static final ImmutableMap<Region, Integer> ROWS =
      new ImmutableMap.Builder<Region, Integer>()
        .put(Region.GBR, 3)
        .put(Region.CAPE_YORK, 4)
        .put(Region.WET_TROPICS, 5)
        .put(Region.BURDEKIN, 6)
        .put(Region.MACKAY_WHITSUNDAY, 7)
        .put(Region.FITZROY, 8)
        .put(Region.BURNETT_MARY, 9)
        .build();


  // Type interpolation would be great here
  final LoadingCache<
    SpreadsheetDataSource,
    Map<java.awt.Color, ProgressTable.Condition>
  > conditionColorCache = CacheBuilder.newBuilder()
      .maximumSize(100)
      .build(
          new CacheLoader<
            SpreadsheetDataSource,
            Map<java.awt.Color, ProgressTable.Condition>
          >() {
            @Override
            public Map<java.awt.Color, ProgressTable.Condition> load(
                SpreadsheetDataSource ds) {
              final Map<java.awt.Color, ProgressTable.Condition> m =
                  Maps.newHashMap();
              try {
                for (int row = 0;; row++) {
                  final Value v = ds.select("condition", row, 0);
                  if (StringUtils.isBlank(v.asString())) {
                    break;
                  }
                  m.put(v.asColor(), getCondition(v.asString()));
                }
              } catch (MissingDataException e) {}
              return m;
            }
          });

  public ProgressTableBuilder() {
    super(Lists.newArrayList(ChartType.PROGRESS_TABLE, ChartType.PROGRESS_TABLE_REGION));
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      return "progress towards targets indicators".equalsIgnoreCase(datasource
          .select("A1").getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
      final Region region, Dimension dimensions) {
    final ProgressTable.Dataset ds;
    try {
      ds = getDataset(datasource, type==ChartType.PROGRESS_TABLE_REGION?region:null);
    } catch (MissingDataException e) {
      // TODO: Handle this better
      throw new RuntimeException(e);
    }
    if (region == Region.GBR || type == ChartType.PROGRESS_TABLE_REGION) {
      return new AbstractChart(dimensions) {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          return new ProgressTable(ds);
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          final StringWriter sw = new StringWriter();
          try {
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            {
              final List<String> cList = Lists.newLinkedList();
              cList.add("");
              for (ProgressTable.Column c : ds.columns) {
                cList.add(c.header);
              }
              csv.write(cList);
            }
            for (ProgressTable.Row r : ds.rows) {
              final List<String> rList = Lists.newLinkedList();
              rList.add(r.header);
              for (ProgressTable.Cell c : r.cells) {
                if (c.condition == null) {
                  rList.add("");
                } else {
                  rList.add(String.format("%s (%s)", c.condition, c.progress));
                }
              }
              csv.write(rList);
            }
            csv.close();
          } catch (IOException e) {
            // How on earth would you get an IOException with a StringWriter?
            throw new RuntimeException(e);
          }
          return sw.toString();
        }

        @Override
        public String getCommentary() throws UnsupportedFormatException {
          return ProgressTableBuilder.this.getCommentary(datasource, region);
        }
      };
    }
    return null;
  }

  private ProgressTable.Dataset getDataset(SpreadsheetDataSource datasource, Region region)
      throws MissingDataException {
    List<ProgressTable.Column> columns = getColumns(datasource, region);
    List<ProgressTable.Row> rows = getRows(columns, datasource, region);
    return new ProgressTable.Dataset(columns, rows);
  }

  private List<ProgressTable.Column> getColumns(SpreadsheetDataSource ds, Region region)
      throws MissingDataException {
    List<ProgressTable.Column> columns = Lists.newArrayList();
    int col = 2;
    while (StringUtils.isNotBlank(ds.select(0, col).asString())) {
      String header = getIndicator(ds.select(0, col).asString(), region).getLabel();
      columns.add(new ProgressTable.Column(header, ds.select(1, col).asString(),
              ds.select(2, col).asString()));
      col++;
    }
    return columns;
  }

  private List<ProgressTable.Row> getRows(List<ProgressTable.Column> columns,
      SpreadsheetDataSource datasource, Region region) throws MissingDataException {
    List<ProgressTable.Row> rows = Lists.newArrayList();
    if(region == null) {
        for (Region r : Region.values()) {
            rows.add(getRow(columns, datasource, r));
        }
    } else {
        rows.add(getRow(columns, datasource, region));
    }
    return rows;
  }

  private ProgressTable.Row getRow(List<ProgressTable.Column> columns,
      SpreadsheetDataSource ds, Region region) throws MissingDataException {
    Integer row = ROWS.get(region);
    if (row == null) {
      throw new RuntimeException("row not configured for region "
          + region.getProperName());
    }
    String r = ds.select(row, 0).asString();
    if (!StringUtils.equals(region.getProperName(), r)) {
      throw new RuntimeException(String.format(
          "expected %s in row %s column 0 but found %s",
          region.getProperName(), row, r));
    }
    List<ProgressTable.Cell> cells = Lists.newArrayList();
    for (ProgressTable.Column column : columns) {
      Double progress = getProgress(ds.select(row, columns.indexOf(column) + 2)
          .asString());
      if (progress != null) {
        ProgressTable.Indicator indicator = getIndicator(column.header, region);
        ProgressTable.Condition condition = getCondition(ds, columns,
            indicator, region);
        cells.add(new ProgressTable.Cell(indicator, condition,
            formatProgress(progress)));
      } else {
        cells.add(new ProgressTable.Cell());
      }
    }
    return new ProgressTable.Row(region.getProperName(), ds.select(row, 1)
        .asString(), cells);
  }

  private Double getProgress(String value) {
    try {
      return Double.parseDouble(value);
    } catch (Exception e) {
      return null;
    }
  }

  private String formatProgress(double value) {
    return Math.round(value * 100.0) + "%";
  }

  private ProgressTable.Indicator getIndicator(String name, Region region) {
    ProgressTable.Indicator i = ProgressTable.Indicator.valueOf(name
        .toUpperCase());
    if (i == null) {
      throw new RuntimeException(String.format("no indicator found for %s",
          name));
    }
    if (region == Region.FITZROY && i == ProgressTable.Indicator.SUGARCANE) {
      return ProgressTable.Indicator.GRAIN;
    }
    return i;
  }

  private ProgressTable.Condition getCondition(SpreadsheetDataSource ds,
      List<ProgressTable.Column> columns, ProgressTable.Indicator indicator,
      Region region) throws MissingDataException {
    if (indicator == ProgressTable.Indicator.GRAIN) {
      indicator = ProgressTable.Indicator.SUGARCANE;
    }
    return getCondition(ds.select("progress", region.ordinal() + 3,
        getConditionColumn(ds, indicator)).asColor(), getConditionColors(ds));
  }

  private ProgressTable.Condition getCondition(
      java.awt.Color c,
      Map<java.awt.Color, ProgressTable.Condition> conditions) {
    if (conditions.containsKey(c)) {
      return conditions.get(c);
    }
    // Not an exact match, so pick closest colour
    final SortedMap<Double, java.awt.Color> m = Maps.newTreeMap();
    for (java.awt.Color otherColor : conditions.keySet()) {
      m.put(distance(c, otherColor), otherColor);
    }
    final java.awt.Color closest = m.get(m.firstKey());
    Logger.debug(String.format("Closest match for %s is %s => %s",
        c, closest, conditions.get(closest)));
    // Get the closest colour
    return conditions.get(closest);
  }

  private ProgressTable.Condition getCondition(String str) {
    return ProgressTable.Condition.valueOf(StringUtils.upperCase(StringUtils
        .replace(str, " ", "")));
  }

  private Map<java.awt.Color, ProgressTable.Condition> getConditionColors(
      SpreadsheetDataSource ds) {
    try {
      return conditionColorCache.get(ds);
    } catch (ExecutionException e) {
      // Shouldn't happen
      throw new RuntimeException(e);
    }
  }

  private int getConditionColumn(SpreadsheetDataSource ds,
      ProgressTable.Indicator indicator) throws MissingDataException {
    for (int col = 2;; col++) {
      String s = ds.select("progress", 0, col).asString();
      if (StringUtils.equalsIgnoreCase(s, indicator.toString())) {
        return col;
      } else if (StringUtils.isBlank(s)) {
        throw new RuntimeException(String.format("indicator %s not found",
            indicator.toString()));
      }
    }
  }

  // From: http://stackoverflow.com/a/2103608/701439
  private double distance(java.awt.Color c1, java.awt.Color c2){
    final double rmean = ( c1.getRed() + c2.getRed() )/2;
    final int r = c1.getRed() - c2.getRed();
    final int g = c1.getGreen() - c2.getGreen();
    final int b = c1.getBlue() - c2.getBlue();
    final double weightR = 2 + rmean/256;
    final double weightG = 4.0;
    final double weightB = 2 + (255-rmean)/256;
    return Math.sqrt(weightR*r*r + weightG*g*g + weightB*b*b);
  }

}
