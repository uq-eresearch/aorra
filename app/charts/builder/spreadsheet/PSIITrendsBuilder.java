package charts.builder.spreadsheet;

import static charts.ChartType.PSII_TRENDS;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.awt.Dimension;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.PSIITrends;

public class PSIITrendsBuilder extends AbstractBuilder {

    private static final String TITLE = "Trends in maximum PSII herbicide equivalent concentrations";

    public PSIITrendsBuilder() {
        super(PSII_TRENDS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.equalsIgnoreCase(datasource.select("A1").asString(), TITLE);
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Chart build(SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions) {
        if(region == Region.GBR) {
            final CategoryDataset dataset = getDataset(datasource);
            final String title = getTitle(datasource);
            return new AbstractChart(queryDimensions) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return PSIITrends.createChart(dataset, title, new Dimension(1500, 750));
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

    private CategoryDataset getDataset(SpreadsheetDataSource ds) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            String cRegion = null;
            String cSite = null;
            for(int row = 2;true;row++) {
                String region = ds.select(row, 0).asString();
                String site = ds.select(row, 1).asString();
                String date = ds.select(row, 2).asString();
                if(isBlank(date)) {
                    break;
                }
                if(isBlank(region)) {
                    region = cRegion;
                } else {
                    cRegion = region;
                }
                if(isBlank(region)) {
                    throw new RuntimeException("no region");
                }
                if(isBlank(site)) {
                    site = cSite;
                } else {
                    cSite = site;
                }
                if(isBlank(site)) {
                    throw new RuntimeException("no site");
                }
                String key = region + PSIITrends.SEPARATOR + site + PSIITrends.SEPARATOR + date;

                for(int i = 3;i<10;i++) {
                    Double val = ds.select(row, i).asDouble();
                    String name = ds.select(1, i).asString();
                    if(val != null && name != null) {
                        dataset.addValue(val, name, key);
                    }
                }
            }
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTitle(SpreadsheetDataSource ds) {
        try {
            int startYear = Integer.MAX_VALUE;
            int endYear = Integer.MIN_VALUE;
            for(int row = 2;true;row++) {
                String date = ds.select(row, 2).asString();
                if(isBlank(date)) {
                    break;
                }
                String[] s = StringUtils.split(date, '-');
                if((s.length == 2) && isInt(s[0]) && isInt(s[1])) {
                    startYear = Math.min(startYear, Integer.parseInt(s[0]));
                    endYear = Math.max(endYear, Integer.parseInt(s[1]));
                }
            }
            return String.format(TITLE+" at all sites monitored from %s to %s", startYear, endYear);
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
