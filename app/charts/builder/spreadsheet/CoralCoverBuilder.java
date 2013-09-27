package charts.builder.spreadsheet;

import static charts.ChartType.CORAL_HCC;
import static charts.ChartType.CORAL_SCC;
import static charts.ChartType.CORAL_MA;
import static charts.ChartType.CORAL_JUV;

import java.awt.Dimension;

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
        Chart chart = null;
        if(region == Region.GBR) {
            final DefaultStatisticalCategoryDataset dataset = getDataset(datasource, type);
            chart = new AbstractChart(queryDimensions) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return CoralCover.createChart(dataset,
                            String.format("%s (mean)", type.getLabel()), new Dimension(750, 500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }

                @Override
                public String getCommentary() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }};
        }
        return chart;
    }

    private DefaultStatisticalCategoryDataset getDataset(SpreadsheetDataSource ds, ChartType type) {
        Integer col = COLUMNS.get(type);
        if(col == null) {
            throw new RuntimeException(String.format("ChartType %s not implemented", type.name()));
        }
        try {
            DefaultStatisticalCategoryDataset d = new DefaultStatisticalCategoryDataset();
            for(int row = 1;true;row++) {
                if(StringUtils.isBlank(ds.select(row, col).asString())) {
                    if(StringUtils.isBlank(ds.select(row+1, col).asString())) {
                        break;
                    } else {
                        continue;
                    }
                }
                double mean = ds.select(row, col).asDouble();
                double deviation = ds.select(row, col+1).asDouble();
                String series = ds.select(row, 0).asString();
                String year = ds.select(row, 1).asInteger().toString();
                d.add(mean, deviation, series, year);
            }
            return d;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

}
