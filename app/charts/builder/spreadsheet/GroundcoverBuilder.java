package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.graphics.Groundcover;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class GroundcoverBuilder extends AbstractBuilder {

    private static final String GC_SHEETNAME = "avgCover_ordered";
    private static final String GCB50_SHEETNAME = "areaBelow50_ordered";

    private static final ImmutableMap<Region, List<Integer>> ROWS =
            new ImmutableMap.Builder<Region, List<Integer>>()
              .put(Region.GBR, Lists.newArrayList(28, 27, 23, 25, 24, 26))
              .put(Region.WET_TROPICS, Lists.newArrayList(2))
              .put(Region.MACKAY_WHITSUNDAY, Lists.newArrayList(3,4,5,6))
              .put(Region.BURDEKIN, Lists.newArrayList(6,7,8,9,10,11))
              .put(Region.FITZROY, Lists.newArrayList(12,13,14,15,16,17))
              .put(Region.BURNETT_MARY, Lists.newArrayList(18,19,20,21,22))
              .build();

    public GroundcoverBuilder() {
        super(Lists.newArrayList(ChartType.GROUNDCOVER, ChartType.GROUNDCOVER_BELOW_50));
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            if(datasource.hasSheet(GC_SHEETNAME) || datasource.hasSheet(GCB50_SHEETNAME)) {
                List<Value> row0 = datasource.selectRow(0);
                List<Value> col1 = datasource.selectColumn(1);
                return SpreadsheetDataSource.containsString(row0, "Region") &&
                        SpreadsheetDataSource.containsString(row0, "Type") &&
                        SpreadsheetDataSource.containsString(row0, "AllYearMean") &&
                        SpreadsheetDataSource.containsString(col1, "Catchment") &&
                        SpreadsheetDataSource.containsString(col1, "Region");
            }
        } catch (MissingDataException e) {}
        return false;
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension dimension) {
        if(!ROWS.containsKey(region)) {
            return null;
        }
        if(type == ChartType.GROUNDCOVER &&
                !StringUtils.equalsIgnoreCase(datasource.getDefaultSheet(), GC_SHEETNAME)) {
            return null;
        }
        if(type == ChartType.GROUNDCOVER_BELOW_50 &&
                !StringUtils.equalsIgnoreCase(datasource.getDefaultSheet(), GCB50_SHEETNAME)) {
            return null;
        }
        Chart chart = new AbstractChart(dimension) {

            @Override
            public ChartDescription getDescription() {
                return new ChartDescription(type, region);
            }

            @Override
            public Drawable getChart() {
                return Groundcover.createChart(
                        createDataset(datasource, type, region),
                        getTitle(datasource, type, region),
                        valueAxisLabel(type),
                        new Dimension(750,500));
            }

            @Override
            public String getCSV() throws UnsupportedFormatException {
              final StringWriter sw = new StringWriter();
              try {
                final CategoryDataset dataset =
                    createDataset(datasource, type, region);
                final CsvListWriter csv = new CsvListWriter(sw,
                    CsvPreference.STANDARD_PREFERENCE);
                @SuppressWarnings("unchecked")
                List<String> columnKeys = dataset.getColumnKeys();
                @SuppressWarnings("unchecked")
                List<String> rowKeys = dataset.getRowKeys();
                final List<String> heading = ImmutableList.<String>builder()
                    .add(format("%s %s", region, type))
                    .addAll(columnKeys)
                    .build();
                csv.write(heading);
                for (String row : rowKeys) {
                  List<String> line = newLinkedList();
                  line.add(row);
                  for (String col : columnKeys) {
                    line.add(format("%.1f",
                        dataset.getValue(row, col).doubleValue()));
                  }
                  csv.write(line);
                }
                csv.close();
              } catch (IOException e) {
                // How on earth would you get an IOException with a StringWriter?
                throw new RuntimeException(e);
              }
              return sw.toString();
            }
        };
        return chart;
    }

    private String valueAxisLabel(ChartType type) {
        if(type == ChartType.GROUNDCOVER) {
            return "Groundcover (%)";
        } else if(type == ChartType.GROUNDCOVER_BELOW_50) {
            return "Area (%)";
        } else {
            throw new RuntimeException(format("type %s not implemented", type.name()));
        }
    }

    private String getTitle(SpreadsheetDataSource ds, ChartType type, Region region) {
        try {
            String start = getStartYear(ds);
            String last = getLastYear(ds);
            if(type == ChartType.GROUNDCOVER) {
                if(region == Region.GBR) {
                    return format("Mean late dry season groundcover in the Great Barrier Reef" +
                        " catchment and regions for %s-%s", start, last);
                } else {
                    return format("Mean late dry season groundcover in the %s" +
                        " catchment for %s-%s", region.getProperName(), start, last);
                }
            } else if(type == ChartType.GROUNDCOVER_BELOW_50) {
                String s = "Percentage of the reporting area with groundcover below 50 per cent in the ";
                if(region == Region.GBR) {
                    return format(s+"Great Barrier Reef catchment and regions for %s-%s", start, last);
                } else {
                    return format(s+"%s catchment for %s-%s", region.getProperName(), start, last);
                }
            }
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(String.format("getTitle not implemented for " +
                "region %s chart type %s", region, type));
    }

    private CategoryDataset createDataset(SpreadsheetDataSource ds, ChartType type, Region region) {
        List<Integer> rows = ROWS.get(region);
        if(rows == null) {
            throw new RuntimeException("no rows configured for region " + region.getProperName());
        }
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(Integer row : rows) {
                row--;
                String rowName = ds.select(row, 0).toString();
                for(int column = 2;column<=getLastColumn(ds);column++) {
                    String series = ds.select(0, column).asInteger().toString();
                    if(ds.select(row, column).getValue() == null) {
                        dataset.addValue(null, rowName, series);
                    } else {
                        dataset.addValue(ds.select(row, column).asDouble(), rowName, series);
                    }
                }
            }
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getStartYear(SpreadsheetDataSource ds) throws MissingDataException {
        return ds.select(0, 2).asInteger().toString();
    }

    public String getLastYear(SpreadsheetDataSource ds) throws MissingDataException {
        return ds.select(0, getLastColumn(ds)).asInteger().toString();
    }

    public int getLastColumn(SpreadsheetDataSource ds) throws MissingDataException {
        for(int col=2;true;col++) {
            String s = ds.select(0, col).asString();
            if(StringUtils.isBlank(s)) {
                return col -1;
            }
            try {
                ds.select(0, col).asInteger();
            } catch(Exception e) {
                return col -1;
            }
        }
    }

}
