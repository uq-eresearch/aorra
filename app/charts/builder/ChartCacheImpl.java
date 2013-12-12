package charts.builder;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import service.EventManager;
import service.OrderedEvent;
import charts.Chart;
import charts.Region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;

public class ChartCacheImpl implements ChartCache {

  private final DefaultChartBuilder chartBuilder;
  private final EventManager eventManager;

  private String lastEventId;

  private final Cache<String, List<Chart>> cache = CacheBuilder
      .newBuilder()
      .maximumSize(maxsize())
      .removalListener(new RemovalListener<String, List<Chart>>() {
        @Override
        public void onRemoval(RemovalNotification<String, List<Chart>> entry) {
          System.out.println(String.format("XXX removing %s charts for id %s from cache",
              entry.getValue().size(), entry.getKey()));
        }})
      .build();

  @Inject
  public ChartCacheImpl(DefaultChartBuilder chartBuilder, EventManager eventManager) {
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
  public List<Chart> getCharts(String id, DataSourceFactory dsf) {
    cleanup();
    List<Chart> clist = cache.getIfPresent(id);
    if(clist == null) {
      clist = actuallyGetCharts(id, dsf);
      cache.put(id, clist);
    }
    return clist;
  }

  private List<Chart> actuallyGetCharts(String id, DataSourceFactory dsf) {
    try {
      return chartBuilder.getCharts(id, dsf, null, Collections.<Region>emptyList(), null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
