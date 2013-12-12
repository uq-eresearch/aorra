package charts.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import service.EventManager;
import service.OrderedEvent;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CachedChartBuilder implements ChartBuilder {

  private Cache<String, List<Chart>> cache = CacheBuilder
      .newBuilder()
      .maximumSize(maxsize())
      .removalListener(new RemovalListener<String, List<Chart>>() {
        @Override
        public void onRemoval(RemovalNotification<String, List<Chart>> entry) {
          System.out.println(String.format("XXX removing %s charts for id %s from cache",
              entry.getValue().size(), entry.getKey()));
        }})
      .build();

  private ChartBuilder chartBuilder;

  private EventManager eventManager;

  private String lastEventId;

  @Inject
  public CachedChartBuilder(DefaultChartBuilder chartBuilder, EventManager eventManager) {
    this.chartBuilder = chartBuilder;
    this.eventManager = eventManager;
  }

  private int maxsize() {
    return Play.application().configuration().getInt("application.ccmaxsize", 100);
  }

  private void cleanup() {
    if(eventManager != null) {
      for(OrderedEvent evt : eventManager.getSince(lastEventId)) {
        if(StringUtils.startsWith(evt.event().type, "file:")) {
          System.out.println(String.format("XXX evt type %s id %s",
              evt.event().type, evt.event().info("id")));
          String id = evt.event().info("id");
          if(id != null) {
            cache.invalidate(id);
          }
        }
      }
      lastEventId = eventManager.getLastEventId();
    }
  }

  @Override
  public synchronized List<Chart> getCharts(String id, DataSourceFactory dsf,
      ChartType type, List<Region> regions, Map<String, String> parameters)
          throws Exception {
    cleanup();
    List<Chart> clist = cache.getIfPresent(id);
    if(clist == null) {
      clist = chartBuilder.getCharts(id, dsf, null, Collections.<Region>emptyList(), null);
      cache.put(id, clist);
    }
    return filter(clist, type, regions, parameters);
  }

  private List<Chart> filter(List<Chart> charts, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    List<Chart> filtered = Lists.newArrayList();
    if(charts != null) {
      for(Chart chart : charts) {
        if(type != null && type != chart.getDescription().getType()) {
          continue;
        }
        if(regions != null && !regions.isEmpty() && !regions.contains(chart.getDescription().getRegion())) {
          continue;
        }
        Map<String, String> cparams = chart.getDescription().getParameters();
        if(parameters != null && cparams != null) {
          for(Map.Entry<String, String> me: parameters.entrySet()) {
            if(cparams.containsKey(me.getKey()) &&
                !StringUtils.equals(me.getValue(), cparams.get(me.getKey()))) {
              continue;
            }
          }
        }
        filtered.add(chart);
      }
    }
    return filtered;
  }
}

