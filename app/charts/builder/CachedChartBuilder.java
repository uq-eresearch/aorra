package charts.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import service.EventManager;
import service.OrderedEvent;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CachedChartBuilder implements ChartBuilder {

  private Map<String, List<Chart>> cache = Maps.newHashMap();

  private ChartBuilder chartBuilder;

  private EventManager eventManager;

  private String lastEventId;

  @Inject
  public CachedChartBuilder(DefaultChartBuilder chartBuilder, EventManager eventManager) {
    this.chartBuilder = chartBuilder;
    this.eventManager = eventManager;
  }

  private void cleanup() {
    if(eventManager != null) {
      for(OrderedEvent evt : eventManager.getSince(lastEventId)) {
        if(StringUtils.startsWith(evt.event().type, "file:")) {
          cache.clear();
          break;
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
    List<Chart> clist = cache.get(id);
    if(clist == null && !cache.containsKey(id)) {
      clist = chartBuilder.getCharts(id, dsf, null, Collections.<Region>emptyList(), null);
      if(clist.isEmpty()) {
        cache.put(id, null);
      } else {
        cache.put(id, clist);
      }
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

