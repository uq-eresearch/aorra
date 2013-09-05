package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.Drawable;
import charts.ProgressTable;
import charts.builder.AbstractChart;
import charts.builder.Chart;
import charts.builder.ChartDescription;
import charts.builder.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Region;
import charts.builder.Chart.UnsupportedFormatException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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

  public ProgressTableBuilder() {
    super(ChartType.PROGRESS_TABLE);
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
  public Chart build(final SpreadsheetDataSource datasource, ChartType type,
      Region region, Map<String, String[]> query) {
    final ProgressTable.Dataset ds;
    try {
      ds = getDataset(datasource);
    } catch (MissingDataException e) {
      // TODO: Handle this better
      throw new RuntimeException(e);
    }
    if (region == Region.GBR) {
      return new AbstractChart(query) {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(ChartType.PROGRESS_TABLE, Region.GBR);
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
          throw new UnsupportedFormatException();
        }
      };
    }
    return null;
  }

  private ProgressTable.Dataset getDataset(SpreadsheetDataSource datasource)
      throws MissingDataException {
    List<ProgressTable.Column> columns = getColumns(datasource);
    List<ProgressTable.Row> rows = getRows(columns, datasource);
    return new ProgressTable.Dataset(columns, rows);
  }

  private List<ProgressTable.Column> getColumns(SpreadsheetDataSource ds)
      throws MissingDataException {
    List<ProgressTable.Column> columns = Lists.newArrayList();
    int col = 2;
    while (StringUtils.isNotBlank(ds.select(0, col).asString())) {
      columns.add(new ProgressTable.Column(ds.select(0, col).asString(), ds
          .select(1, col).asString(), ds.select(2, col).asString()));
      col++;
    }
    return columns;
  }

  private List<ProgressTable.Row> getRows(List<ProgressTable.Column> columns,
      SpreadsheetDataSource datasource) throws MissingDataException {
    List<ProgressTable.Row> rows = Lists.newArrayList();
    for (Region region : Region.values()) {
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
    return getCondition(ds.select("condition", region.ordinal() + 1,
        getConditionColumn(ds, indicator)).asString());
  }

  private ProgressTable.Condition getCondition(String str) {
    return ProgressTable.Condition.valueOf(StringUtils.upperCase(StringUtils
        .replace(str, " ", "")));
  }

  private int getConditionColumn(SpreadsheetDataSource ds,
      ProgressTable.Indicator indicator) throws MissingDataException {
    int col = 1;
    while (true) {
      String s = ds.select("condition", 0, col).asString();
      if (StringUtils.equalsIgnoreCase(s, indicator.toString())) {
        return col;
      } else if (StringUtils.isBlank(s)) {
        throw new RuntimeException(String.format("indicator %s not found",
            indicator.toString()));
      }
      col++;
    }
  }

}
