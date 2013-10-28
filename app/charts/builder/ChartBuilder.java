package charts.builder;

import java.awt.Dimension;
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
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

@Singleton
public class ChartBuilder {

  private final List<ChartTypeBuilder> builders = detectBuilders();

  public List<Chart> getCharts(List<DataSource> datasources,
          ChartType type,
          List<Region> regions,
          Dimension dimensions,
          Map<String, String> parameters) {
    final List<Chart> result = Lists.newLinkedList();
    for (final ChartTypeBuilder builder : builders) {
      if (builder.canHandle(type, datasources)) {
        result.addAll(builder.build(datasources, type, regions, dimensions, parameters));
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

  public List<Chart> getCharts(List<DataSource> datasources,
      List<Region> regions,
      Dimension dimensions) {
    return getCharts(datasources, null, regions, dimensions, null);
  }

  public Map<String, List<String>> getParameters(List<DataSource> datasources, ChartType type) {
      Map<String, List<String>> result = Maps.newHashMap();
      for(ChartTypeBuilder builder : builders) {
          if(builder.canHandle(type, datasources)) {
              result.putAll(builder.getParameters(datasources, type));
          }
      }
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
