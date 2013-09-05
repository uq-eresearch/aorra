package charts.builder.spreadsheet;

import java.util.Map;

import org.jfree.data.category.CategoryDataset;

import charts.Drawable;
import charts.GrazingPracticeSystems;
import charts.builder.AbstractChart;
import charts.builder.Chart;
import charts.builder.ChartDescription;
import charts.builder.ChartType;
import charts.builder.Region;

import com.google.common.collect.ImmutableMap;

public class GrazingPracticeSystemsBuilder extends
        AbstractBuilder {

    private static final String TITLE = "Grazing practice systems";

    private static final ImmutableMap<Region, Integer> ROWS =
            new ImmutableMap.Builder<Region, Integer>()
              .put(Region.GBR, 1)
              .put(Region.CAPE_YORK, 5)
              .put(Region.WET_TROPICS, 9)
              .put(Region.BURDEKIN, 13)
              .put(Region.MACKAY_WHITSUNDAY, 17)
              .put(Region.FITZROY, 21)
              .put(Region.BURNETT_MARY, 25)
              .build();

    public GrazingPracticeSystemsBuilder() {
        super(ChartType.GPS);
    }

    @Override
    boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return "grazing practice systems".equalsIgnoreCase(
                    datasource.select("A1").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    Chart build(final SpreadsheetDataSource datasource, final ChartType type, final Region region,
            final Map<String, String[]> query) {
        return new AbstractChart(query) {
          @Override
          public ChartDescription getDescription() {
            return new ChartDescription(type, region);
          }
          @Override
          public Drawable getChart() {
            return GrazingPracticeSystems.createChart(createDataset(datasource, region), TITLE,
                    getChartSize(query, 750, 500));
          }
          @Override
          public String getCSV() throws UnsupportedFormatException {
              throw new RuntimeException("not implemented");
          }
        };
    }

    private CategoryDataset createDataset(SpreadsheetDataSource datasource, Region region) {
        return null;
    }

}
