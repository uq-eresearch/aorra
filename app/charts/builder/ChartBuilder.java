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
import charts.builder.ChartCache.CacheEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChartBuilder {

  private final List<ChartTypeBuilder> builders = detectBuilders();

  private final ChartCache cache;

  @Inject
  public ChartBuilder(ChartCache cache) {
    this.cache = cache;
  }

  public List<Chart> getCharts(String id, DataSourceFactory dsf, ChartType type,
      List<Region> regions, Map<String, String> parameters) throws Exception {
    final List<Chart> result = Lists.newLinkedList();
    List<CacheEntry> clist = cache.get(id, type, regions, parameters);
    if(clist == null) {
      DataSource datasource = dsf.getDataSource(id);
      List<Chart> charts = getCharts(datasource,
          null, Collections.<Region>emptyList(), null);
      if(charts.isEmpty()) {
        cache.noCharts(id);
      } else {
        for(Chart chart : charts) {
          cache.add(id, chart);
        }
      }
      clist = cache.get(id, type, regions, parameters);
    }
    if(clist == null) {
      throw new RuntimeException("cache load");
    }
    for(CacheEntry entry : clist) {
      if(entry.chart() != null) {
        result.add(entry.chart());
      }
    }
    return result;
  }

  private List<Chart> getCharts(DataSource datasource,
      ChartType type,
      List<Region> regions,
      Map<String, String> parameters) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : builders) {
      try {
        result.addAll(builder.build(datasource, type, regions, parameters));
      } catch(Exception e) {
          e.printStackTrace();
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

  private static List<ChartTypeBuilder> detectBuilders() {
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
