package charts.builder;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import charts.Chart;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.AnnualRainfallBuilder;
import charts.builder.spreadsheet.CotsOutbreakBuilder;
import charts.builder.spreadsheet.GrazingPracticeSystemsBuilder;
import charts.builder.spreadsheet.LandPracticeSystemsBuilder;
import charts.builder.spreadsheet.MarineBuilder;
import charts.builder.spreadsheet.ProgressTableBuilder;
import charts.builder.spreadsheet.TrackingTowardsTagetsBuilder;
import charts.builder.spreadsheet.TrendsSeagrassAbundanceBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

@Singleton
public class ChartBuilder {

  private final List<ChartTypeBuilder> builders =
    new ImmutableList.Builder<ChartTypeBuilder>()
      .add(new MarineBuilder())
      .add(new CotsOutbreakBuilder())
      .add(new AnnualRainfallBuilder())
      .add(new ProgressTableBuilder())
      .add(new TrackingTowardsTagetsBuilder())
      .add(new GrazingPracticeSystemsBuilder())
      .add(new LandPracticeSystemsBuilder())
      .add(new TrendsSeagrassAbundanceBuilder())
      .build();

  public List<Chart> getCharts(List<DataSource> datasources,
          ChartType type, Map<String, String[]> query) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : builders) {
      if (builder.canHandle(type, datasources)) {
        result.addAll(builder.build(datasources, type, query));
      }
    }
    // make sure charts are sorted by region
    // https://github.com/uq-eresearch/aorra/issues/44
    Collections.sort(result, new Comparator<Chart>() {
      @Override
      public int compare(Chart c1, Chart c2) {
        return getRegion(c1).compareTo(getRegion(c2));
      }
      private Region getRegion(Chart c) {
        return c.getDescription().getRegion();
      }
    });
    return result;
  }

  public List<Chart> getCharts(List<DataSource> datasources, Map<String, String[]> query) {
    return getCharts(datasources, null, query);
  }

}
