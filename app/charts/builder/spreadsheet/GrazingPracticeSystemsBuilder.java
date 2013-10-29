package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import charts.graphics.GrazingPracticeSystems;

import com.google.common.collect.ImmutableList;
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
      public String getCSV() {
        final StringWriter sw = new StringWriter();
        try {
          final CategoryDataset dataset = createDataset(datasource, region);
          final CsvListWriter csv = new CsvListWriter(sw,
              CsvPreference.STANDARD_PREFERENCE);
          @SuppressWarnings("unchecked")
          List<String> columnKeys = dataset.getColumnKeys();
          @SuppressWarnings("unchecked")
          List<String> rowKeys = dataset.getRowKeys();
          final List<String> heading = ImmutableList.<String>builder()
              .add(region+" grazing practices")
              .addAll(rowKeys)
              .build();
          csv.write(heading); // Heading, 2008-2009, 2009-2010
          for (String col : columnKeys) {
            List<String> line = newLinkedList();
            line.add(col);
            for (String row : rowKeys) {
              line.add(format("%.1f",
                  dataset.getValue(row, col).doubleValue() * 100));
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
