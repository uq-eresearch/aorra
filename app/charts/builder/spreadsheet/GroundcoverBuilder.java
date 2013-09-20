package charts.builder.spreadsheet;

import static java.lang.String.format;
import java.awt.Dimension;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.Groundcover;

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
        return datasource.hasSheet(GC_SHEETNAME) ||
                datasource.hasSheet(GCB50_SHEETNAME);
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension dimension) {
        if(!ROWS.containsKey(region)) {
            return null;
        }
        if(!datasource.hasSheet(sheetname(type))) {
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
                throw new UnsupportedFormatException();
            }

            @Override
            public String getCommentary() throws UnsupportedFormatException {
                throw new UnsupportedFormatException();
            }};
        return chart;
    }

    private String sheetname(ChartType type) {
        if(type == ChartType.GROUNDCOVER) {
            return GC_SHEETNAME;
        } else if(type == ChartType.GROUNDCOVER_BELOW_50) {
            return GCB50_SHEETNAME;
        } else {
            return null;
        }
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
            String sheetname = sheetname(type);
            String start = getStartYear(ds, sheetname);
            String last = getLastYear(ds,sheetname);
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
            String sheetname = sheetname(type);
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(Integer row : rows) {
                row--;
                String rowName = ds.select(sheetname, row, 0).toString();
                for(int column = 2;column<=getLastColumn(ds, sheetname);column++) {
                    String series = ds.select(sheetname, 0, column).asInteger().toString();
                    if(ds.select(sheetname, row, column).getValue() == null) {
                        dataset.addValue(null, rowName, series);
                    } else {
                        dataset.addValue(ds.select(sheetname, row, column).asDouble(), rowName, series);
                    }
                }
            }
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getStartYear(SpreadsheetDataSource ds, String sheetname) throws MissingDataException {
        return ds.select(sheetname, 0, 2).asInteger().toString();
    }

    public String getLastYear(SpreadsheetDataSource ds, String sheetname) throws MissingDataException {
        return ds.select(sheetname, 0, getLastColumn(ds, sheetname)).asInteger().toString();
    }

    public int getLastColumn(SpreadsheetDataSource ds, String sheetname) throws MissingDataException {
        for(int col=2;true;col++) {
            String s = ds.select(sheetname, 0, col).asString();
            if(StringUtils.isBlank(s)) {
                return col -1;
            }
            try {
                ds.select(sheetname, 0, col).asInteger();
            } catch(Exception e) {
                return col -1;
            }
        }
    }

}
