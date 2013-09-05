package charts.builder.spreadsheet;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.Drawable;
import charts.GrazingPracticeSystems;
import charts.builder.AbstractChart;
import charts.builder.Chart;
import charts.builder.ChartDescription;
import charts.builder.ChartType;
import charts.builder.Region;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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
        return TITLE.equalsIgnoreCase(StringUtils.strip(datasource.select("A1").asString()));
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
            return GrazingPracticeSystems.createChart(createDataset(datasource, region),
                    TITLE + " - " + region.getProperName(),
                    getChartSize(query, 750, 500));
          }
          @Override
          public String getCSV() throws UnsupportedFormatException {
              throw new RuntimeException("not implemented");
          }
        };
    }

    private CategoryDataset createDataset(SpreadsheetDataSource ds, Region region) {
        Integer row = ROWS.get(region);
        if(row == null) {
            throw new RuntimeException(String.format("region %s not supported", region));
        }
        row++;
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> categories = Lists.newArrayList("A", "B", "C", "D");
        for(int i = 0; i<2;i++) {
            String series = ds.select(row+i, 0).asString();
            for(int col=0;col<4;col++) {
                dataset.addValue(ds.select(row+i, col+1).asDouble(), series, categories.get(col));
            }
        }
        return dataset;
    }

}
