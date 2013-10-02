package charts.builder.spreadsheet;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.strip;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.TrendsSeagrassAbundance;

import com.google.common.collect.Lists;

public class TrendsSeagrassAbundanceBuilder extends AbstractBuilder {

    private static final String SUBREGION = "subregion";

    public TrendsSeagrassAbundanceBuilder() {
        super(ChartType.TSA);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        return getSheet(ds) != -1;
    }

    private boolean stripEqualsIgnoreCase(SpreadsheetDataSource ds, String s,
            String sheet, String cellref) throws MissingDataException {
        return equalsIgnoreCase(s, strip(ds.select(sheet, cellref).asString()));
    }

    private int getSheet(SpreadsheetDataSource ds) {
        for(int i=0;i<ds.sheets();i++) {
            try {
                String sheet = ds.getSheetname(i);
                if(stripEqualsIgnoreCase(ds, "site", sheet, "A1") && 
                   stripEqualsIgnoreCase(ds, "date", sheet, "B1") &&
                   stripEqualsIgnoreCase(ds, "mean", sheet, "C1") &&
                   stripEqualsIgnoreCase(ds, "se", sheet, "D1")) {
                    return i;
                }
            } catch(MissingDataException e) {}
        }
        return -1;
    }

    @Override
    protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
        return Collections.singletonMap(SUBREGION, getSubregions(datasource));
    }

    private List<String> getSubregions(SpreadsheetDataSource ds) {
        List<String> subregions = Lists.newArrayList();
        for(int row = 1; true; row++) {
            try {
                String s = strip(ds.select(row, 0).asString());
                if(isBlank(s)) {
                    if(isBlank(strip(ds.select(row+1, 0).asString()))) {
                        break;
                    } else {
                        continue;
                    }
                }
                if(!subregions.contains(s)) {
                    subregions.add(s);
                }
            } catch(MissingDataException e ) {}
        }
        return subregions;
    }

    private String getSubregion(final SpreadsheetDataSource ds, Map<String, ?> parameters) {
        String subregion = (String)parameters.get(SUBREGION);
        if(isBlank(subregion)) {
            return null;
        }
        if(!getSubregions(ds).contains(subregion)) {
            return null;
        }
        return subregion;
    }

    @Override
    public Chart build(final SpreadsheetDataSource ds, final ChartType type,
        final Region region, Dimension dimensions, final Map<String, ?> parameters) {
        if(region == Region.GBR) {
            int sheet = getSheet(ds);
            if(sheet != -1) {
                ds.setDefaultSheet(sheet);
            } else {
                return null;
            }
            String subregion = getSubregion(ds, parameters);
            if(subregion == null) {
                return null;
            }
            final CategoryDataset dataset = createDataset(ds, subregion);
            final String title = getTitle(subregion);
            return new AbstractChart(dimensions) {
                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region, parameters);
                }

                @Override
                public Drawable getChart() {
                    return TrendsSeagrassAbundance.createChart(
                            dataset, title, new Dimension(750, 500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }

                @Override
                public String getCommentary() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }
            };
        }
        return null;
    }

    private int getSubregionRowStart(SpreadsheetDataSource ds, String subregion) {
        for(int row = 1; true; row++) {
            try {
                String s = strip(ds.select(row, 0).asString());
                if(isBlank(s)) {
                    if(isBlank(strip(ds.select(row+1, 0).asString()))) {
                        break;
                    } else {
                        continue;
                    }
                }
                if(equalsIgnoreCase(s, subregion)) {
                    return row;
                }
            } catch(MissingDataException e ) {}
        }
        return -1;
    }

    private CategoryDataset createDataset(SpreadsheetDataSource ds, String subregion) {
        DefaultStatisticalCategoryDataset d = new DefaultStatisticalCategoryDataset();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMM yyyy");
        try {
            int row = getSubregionRowStart(ds, subregion);
            if(row == -1) {
                return d;
            }
            for(;true;row++) {
                String subr = strip(ds.select(row, 0).asString());
                if(!equalsIgnoreCase(subr, subregion)) {
                    break;
                }
                Date date = ds.select(row, 1).asDate();
                Double mean = ds.select(row, 2).asDouble();
                Double deviation = ds.select(row, 3).asDouble();
                if(mean != null && deviation != null) {
                    d.add(mean, deviation, subregion, sdf.format(date));
                }
            }
        } catch(MissingDataException e) {
            e.printStackTrace();
        }
        return d;
    }

    private String getTitle(String subregion) {
        return String.format("Trends in seagrass abundance (mean) at %s", subregion);
    }

}
