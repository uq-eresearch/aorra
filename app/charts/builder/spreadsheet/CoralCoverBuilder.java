package charts.builder.spreadsheet;

import static charts.ChartType.CORAL_HCC;
import static charts.ChartType.CORAL_JUV;
import static charts.ChartType.CORAL_MA;
import static charts.ChartType.CORAL_SCC;

import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.CoralCover;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CoralCoverBuilder extends AbstractBuilder {
    
    private static final String HC_MEAN = "HC mean";
    private static final String HC_SE = "HC se";

    private static final ImmutableMap<ChartType, Integer> COLUMNS =
            new ImmutableMap.Builder<ChartType, Integer>()
              .put(CORAL_HCC, 2)
              .put(CORAL_SCC, 4)
              .put(CORAL_MA, 6)
              .put(CORAL_JUV, 8)
              .build();

    private static final  ImmutableMap<String, Region> REGIONS = ImmutableMap.of(
             "Wet Tropics", Region.WET_TROPICS,
             "Burdekin", Region.BURDEKIN,
             "Whitsunday", Region.MACKAY_WHITSUNDAY,
             "Fitzroy", Region.FITZROY);

    public CoralCoverBuilder() {
        super(Lists.newArrayList(CORAL_HCC, CORAL_SCC, CORAL_MA, CORAL_JUV));
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            return StringUtils.equalsIgnoreCase(ds.select(0, 2).asString(), HC_MEAN) && 
                    StringUtils.equalsIgnoreCase(ds.select(0, 3).asString(), HC_SE);
        } catch(MissingDataException e) {
            return false;
        }
    }

    @Override
    public Chart build(SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions) {
        if((region == Region.GBR) || containsRegion(datasource, region)) {
            final DefaultStatisticalCategoryDataset dataset = getDataset(datasource, type, region);
            return new AbstractChart(queryDimensions) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return CoralCover.createChart(dataset, type, region, new Dimension(750, 500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }

                @Override
                public String getCommentary() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }};
        } else {
            return null;
        }
    }

    private boolean eof(SpreadsheetDataSource ds, int row) throws MissingDataException {
        if(StringUtils.isBlank(ds.select(row, 0).asString()) &&
                StringUtils.isBlank(ds.select(row+1, 0).asString())) {
            return true;
        } else {
            return false;
        }
    }

    private List<Region> regions(SpreadsheetDataSource ds) {
        try {
            Set<Region> regions = Sets.newLinkedHashSet();
            for(int row=0;!eof(ds, row); row++) {
                if(REGIONS.containsKey(ds.select(row, 0).asString())) {
                    regions.add(REGIONS.get(ds.select(row, 0).asString()));
                }
            }
            return Lists.newArrayList(regions);
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsRegion(SpreadsheetDataSource ds, Region region) {
        return regions(ds).contains(region);
    }

    private DefaultStatisticalCategoryDataset getDataset(SpreadsheetDataSource ds,
            ChartType type, Region region) {
        Integer col = COLUMNS.get(type);
        if(col == null) {
            throw new RuntimeException(String.format("ChartType %s not implemented", type.name()));
        }
        List<Region> regions;
        if(region == Region.GBR) {
            regions = regions(ds);
        } else {
            if(containsRegion(ds, region)) {
                regions = Collections.singletonList(region);
            } else {
                throw new RuntimeException(String.format("no entries for region %s",
                        region.getProperName()));
            }
        }
        try {
            DefaultStatisticalCategoryDataset d = new DefaultStatisticalCategoryDataset();
            for(Region r : regions) {
                for(int row = getRegionStart(ds, r);
                        StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
                    double mean = ds.select(row, col).asDouble();
                    double deviation = ds.select(row, col+1).asDouble();
                    String series = ds.select(row, 0).asString();
                    String year = ds.select(row, 1).asInteger().toString();
                    d.add(mean, deviation, series, year);
                }
            }
            return d;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private int getRegionStart(SpreadsheetDataSource ds, Region region) throws MissingDataException {
        for(int row = 0;!eof(ds, row);row++) {
            if(REGIONS.get(ds.select(row, 0).asString()) == region) {
                return row;
            }
        }
        throw new RuntimeException(String.format("region %s not found", region.getProperName()));
    }

}
