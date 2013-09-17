package charts.builder.spreadsheet;

import java.text.SimpleDateFormat;
import java.util.Date;
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

public class TrendsSeagrassAbundanceBuilder extends AbstractBuilder {

    public TrendsSeagrassAbundanceBuilder() {
        super(ChartType.TSA);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return "trends in seagrass abundance".equalsIgnoreCase(datasource.select("A1")
                .getValue());
          } catch (MissingDataException e) {
            return false;
          }
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, final Map<String, String[]> query) {
        if(region == Region.GBR) {
            return new AbstractChart(query) {
                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return TrendsSeagrassAbundance.createChart(
                            createDataset(datasource),
                            getTitle(datasource),
                            getChartSize(query, 750, 500));
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

    private CategoryDataset createDataset(SpreadsheetDataSource ds) {
        DefaultStatisticalCategoryDataset d = new DefaultStatisticalCategoryDataset();
        try {
            String row = ds.select(1, 0).asString();
            SimpleDateFormat sdf = new SimpleDateFormat("MMMMM yyyy");
            for(int i=1;true;i++) {
                Date date = ds.select(0, i).asDate();
                if(date == null) {
                    break;
                }
                double mean = ds.select(1, i).asDouble();
                double deviation = ds.select(2, i).asDouble();
                
                d.add(mean, deviation, row, sdf.format(date));
            }
        } catch(MissingDataException e) {
            e.printStackTrace();
        }
        return d;
    }

    private String getTitle(SpreadsheetDataSource ds) {
        try {
            return ds.select(1, 0).asString();
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

}
