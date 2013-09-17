package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import play.api.templates.Html;
import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.Chart.UnsupportedFormatException;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.GrazingPracticeSystems;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class GrazingPracticeSystemsBuilder extends AbstractBuilder {

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
    super(ChartType.GRAZING_PS);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      return TITLE.equalsIgnoreCase(StringUtils.strip(datasource.select("A1")
          .asString()));
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public Chart build(final SpreadsheetDataSource datasource,
      final ChartType type, final Region region,
      final Dimension dimensions) {
    final AbstractBuilder thisBuilder = this;
    return new AbstractChart(dimensions) {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(type, region);
      }

      @Override
      public Drawable getChart() {
        return GrazingPracticeSystems.createChart(
            createDataset(datasource, region),
            TITLE + " - " + region.getProperName(),
            new Dimension(750, 500));
      }

      @Override
      public String getCSV() throws UnsupportedFormatException {
        throw new UnsupportedFormatException();
      }

      @Override
      public String getCommentary() throws UnsupportedFormatException {
        return thisBuilder.getCommentary(datasource, region);
      }
    };
  }

  private CategoryDataset
      createDataset(SpreadsheetDataSource ds, Region region) {
    Integer row = ROWS.get(region);
    if (row == null) {
      // TODO: Handle this better
      throw new RuntimeException(String.format("region %s not supported",
          region));
    }
    row++;
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    List<String> categories = Lists.newArrayList("A", "B", "C", "D");
    try {
      for (int i = 0; i < 2; i++) {
        String series = ds.select(row + i, 0).asString();
        for (int col = 0; col < 4; col++) {
          dataset.addValue(ds.select(row + i, col + 1).asDouble(), series,
              categories.get(col));
        }
      }
    } catch (MissingDataException e) {
      // TODO: Handle this better
      throw new RuntimeException(e);
    }
    return dataset;
  }

}
