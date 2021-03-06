package charts.builder;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.reflections.Reflections;

import play.Logger;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultChartBuilder implements ChartBuilder {

  private static final ImmutableList<ChartTypeBuilder> builders = detectBuilders();

  private final DataSourceFactory dataSourceFactory;

  @Inject
  public DefaultChartBuilder(DataSourceFactory dsf) {
    this.dataSourceFactory = dsf;
  }

  @Override
  public List<Chart> getCharts(String id,  ChartType type,
      List<Region> regions, Map<String, String> parameters) throws Exception {
    DataSource datasource = dataSourceFactory.getDataSource(id);
    return getCharts(datasource, type, regions, parameters);
  }

  private List<Chart> getCharts(DataSource datasource,
      ChartType type,
      List<Region> regions,
      Map<String, String> parameters) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : builders) {
      try {
        List<Chart> charts = builder.build(datasource, type, regions, parameters);
        if(!charts.isEmpty()) {
          result.addAll(charts);
        }
      } catch(Exception e) {
        Logger.warn(String.format("caught exception while building charts (type %s," +
            " regions %s, parameters %s)", type, regions, parameters), e);
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

  public List<ChartTypeBuilder> builders() {
    return builders;
  }

  private static ImmutableList<ChartTypeBuilder> detectBuilders() {
    final ImmutableList.Builder<ChartTypeBuilder> b =
        new ImmutableList.Builder<ChartTypeBuilder>();
    for (Class<? extends ChartTypeBuilder> builderClass : new Reflections(
        "charts.builder").getSubTypesOf(ChartTypeBuilder.class)) {
      if (builderClass.isInterface())
        continue;
      if (Modifier.isAbstract(builderClass.getModifiers()))
        continue;
      try {
        Logger.debug("Found chart builder: "+builderClass.getCanonicalName());
        b.add(builderClass.newInstance());
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return b.build();
  }
}
