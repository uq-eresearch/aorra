package charts.builder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ChartBuilder {

  private static List<ChartTypeBuilder> BUILDERS =
    new ImmutableList.Builder<ChartTypeBuilder>()
      .add(new MarineSpreadsheetChartBuilder())
      .add(new CotsOutbreakSpreadsheetBuilder())
      .add(new AnnualRainfallChartBuilder())
      .add(new ProgressTableChartBuilder())
      .add(new TrackingTowardsTagetsBuilder())
      .build();

  private final List<DataSource> datasources;

  public ChartBuilder(List<DataSource> datasources) {
    this.datasources = datasources;
  }

  public List<Chart> getCharts(ChartType type, Map<String, String[]> query) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : BUILDERS) {
      if (builder.canHandle(type, datasources)) {
        result.addAll(builder.build(datasources, type, query));
      }
    }
    // make sure charts are sorted by region
    // https://github.com/uq-eresearch/aorra/issues/44
    Collections.sort(result, new Comparator<Chart>() {
      @Override
      public int compare(Chart c1, Chart c2) {
        if (getRegion(c1) == null) {
          if (getRegion(c2) == null)
            return 0;
          return -1;
        } else {
          return getRegion(c1).compareTo(getRegion(c2));
        }
      }
      private Region getRegion(Chart c) {
        if (c.getDescription() == null)
          return null;
        return c.getDescription().getRegion();
      }
    });
    return result;
  }

  public List<Chart> getCharts(Map<String, String[]> query) {
    return getCharts(null, query);
  }

}
