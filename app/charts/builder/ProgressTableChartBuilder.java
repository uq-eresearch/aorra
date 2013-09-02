package charts.builder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.Drawable;
import charts.ProgressTable;
import charts.spreadsheet.DataSource;
import charts.spreadsheet.SpreadsheetHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class ProgressTableChartBuilder extends DefaultSpreadsheetChartBuilder {

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

    public ProgressTableChartBuilder() {
        super(ChartType.PROGRESS_TABLE);
    }

    @Override
    boolean canHandle(DataSource datasource) {
        try {
            return "progress towards targets indicators".equalsIgnoreCase(
                    datasource.select("A1").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    Chart build(final DataSource datasource, ChartType type, Region region,
            Map<String, String[]> query) {
      final ProgressTable.Dataset ds = getDataset(datasource);
      if(region == Region.GBR) {
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
                    rList.add(String.format("%s (%s)",
                        c.condition, c.progress));
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
        };
      }
      return null;
    }

    private ProgressTable.Dataset getDataset(DataSource datasource) {
        List<ProgressTable.Column> columns = getColumns(datasource);
        List<ProgressTable.Row> rows = getRows(columns, datasource);
        return new ProgressTable.Dataset(columns, rows);
    }

    private List<ProgressTable.Column> getColumns(DataSource datasource) {
        SpreadsheetHelper helper = new SpreadsheetHelper(datasource);
        List<ProgressTable.Column> columns = Lists.newArrayList();
        int col = 2;
        while(StringUtils.isNotBlank(helper.selectText(0, col))) {
            columns.add(new ProgressTable.Column(helper.selectText(0, col),
                    helper.selectText(1, col), helper.selectText(2, col)));
            col++;
        }
        return columns;
    }

    private List<ProgressTable.Row> getRows(List<ProgressTable.Column> columns , DataSource datasource) {
        List<ProgressTable.Row> rows = Lists.newArrayList();
        for(Region region : Region.values()) {
            rows.add(getRow(columns, datasource, region));
        }
        return rows;
    }

    private ProgressTable.Row getRow(List<ProgressTable.Column> columns,
            DataSource datasource, Region region) {
        SpreadsheetHelper helper = new SpreadsheetHelper(datasource);
        Integer row = ROWS.get(region);
        if(row == null) {
            throw new RuntimeException("row not configured for region "+region.getProperName());
        }
        String r = helper.selectText(row, 0);
        if(!StringUtils.equals(region.getProperName(), r)) {
            throw new RuntimeException(String.format("expected %s in row %s column 0 but found %s",
                region.getProperName(), row, r));
        }
        List<ProgressTable.Cell> cells = Lists.newArrayList();
        for(ProgressTable.Column column : columns) {
            Double progress = getProgress(helper.selectText(row, columns.indexOf(column)+2));
            if(progress != null) {
                ProgressTable.Indicator indicator = getIndicator(column.header, region);
                ProgressTable.Condition condition = getCondition(helper, columns, indicator, region);
                cells.add(new ProgressTable.Cell(indicator, condition , formatProgress(progress)));
            } else {
                cells.add(new ProgressTable.Cell());
            }
        }
        return new ProgressTable.Row(region.getProperName(), helper.selectText(row, 1), cells);
    }

    private Double getProgress(String value) {
        try {
            return Double.parseDouble(value);
        } catch(Exception e) {
            return null;
        }
    }

    private String formatProgress(double value) {
        return Math.round(value*100.0)+"%";
    }

    private ProgressTable.Indicator getIndicator(String name, Region region) {
        ProgressTable.Indicator i = ProgressTable.Indicator.valueOf(name.toUpperCase());
        if(i == null) {
            throw new RuntimeException(String.format("no indicator found for %s", name));
        }
        if(region == Region.FITZROY && i == ProgressTable.Indicator.SUGARCANE) {
            return ProgressTable.Indicator.GRAIN;
        }
        return i;
    }

    private ProgressTable.Condition getCondition(SpreadsheetHelper helper,
            List<ProgressTable.Column> columns, ProgressTable.Indicator indicator, Region region) {
        if(indicator == ProgressTable.Indicator.GRAIN) {
            indicator = ProgressTable.Indicator.SUGARCANE;
        }
        return getCondition(helper.selectText(
                "condition", region.ordinal()+1, getConditionColumn(helper, indicator)));
    }

    private ProgressTable.Condition getCondition(String str) {
        return ProgressTable.Condition.valueOf(StringUtils.upperCase(StringUtils.replace(str, " ", "")));
    }

    private int getConditionColumn(SpreadsheetHelper helper, ProgressTable.Indicator indicator) {
        int col = 1;
        while(true) {
            String s = helper.selectText("condition", 0, col);
            if(StringUtils.equalsIgnoreCase(s, indicator.toString())) {
                return col;
            } else if(StringUtils.isBlank(s)) {
                throw new RuntimeException(String.format("indicator %s not found", indicator.toString()));
            }
            col++;
        }
    }

}
