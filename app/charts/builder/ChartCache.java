package charts.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import service.EventManager;
import service.OrderedEvent;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Region;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChartCache {

  public static class Key {
    private String id;
    private ChartType type;
    private Region region;
    private Map<String, String> parameters;

    public Key(String id) {
      if(id == null) {
        throw new IllegalArgumentException("datasource id is null");
      }
      this.id = id;
    }

    public Key(String id, ChartType type, Region region, Map<String, String> parameters) {
      this(id);
      this.type = type;
      this.region = region;
      this.parameters = parameters;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result
          + ((parameters == null) ? 0 : parameters.hashCode());
      result = prime * result + ((region == null) ? 0 : region.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Key other = (Key) obj;
      if (id == null) {
        if (other.id != null)
          return false;
      } else if (!id.equals(other.id))
        return false;
      if (parameters == null) {
        if (other.parameters != null)
          return false;
      } else if (!parameters.equals(other.parameters))
        return false;
      if (region != other.region)
        return false;
      if (type != other.type)
        return false;
      return true;
    }

    public boolean matches(String id, ChartType type, List<Region> regions,
        Map<String, String> parameters) {
      if(!StringUtils.equals(id, this.id)) {
        return false;
      }
      if(type != null && type != this.type) {
        return false;
      }
      if(regions != null && !regions.isEmpty() && !regions.contains(region)) {
        return false;
      }
      if(parameters != null && this.parameters != null) {
        for(Map.Entry<String, String> me: parameters.entrySet()) {
          if(this.parameters.containsKey(me.getKey()) &&
              !StringUtils.equals(me.getValue(), this.parameters.get(me.getKey()))) {
            return false;
          }
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return id+"/"+(type!=null?type.name():"")+"/"+(region!=null?region.name():"")+"/"+
          (parameters!=null?parameters.toString():"");
    }
  }

  public static class CacheEntry {

    private Key key;

    private Chart chart;

    public CacheEntry(Key key, Chart chart) {
      this.key = key;
      this.chart = chart;
    }

    public Chart chart() {
      return this.chart;
    }

    public Key key() {
      return key;
    }
  }

  private Map<String, List<CacheEntry>> cache = Maps.newHashMap();

  private EventManager eventManager;

  private String lastEventId;

  @Inject
  public ChartCache(EventManager eventManager) {
    this.eventManager = eventManager;
  }

  public synchronized void noCharts(String id) {
    add(id, null);
  }

  public synchronized void add(String id, Chart chart) {
    cleanup();
    List<CacheEntry> clist = cache.get(id);
    if(clist == null) {
      clist = Lists.newArrayList();
      cache.put(id, clist);
    }
    if(chart == null) {
      clist.add(new CacheEntry(new Key(id), null));
    } else {
      ChartDescription d = chart.getDescription();
      Iterator<CacheEntry> ceIter = clist.iterator();
      Key k = new Key(id, d.getType(), d.getRegion(),d.getParameters());
      while(ceIter.hasNext()) {
        CacheEntry ce = ceIter.next();
        if(ce.key().equals(k)) {
          ceIter.remove();
        }
      }
      clist.add(new CacheEntry(k, chart));
    }
  }

  public synchronized List<CacheEntry> get(String id, ChartType type, List<Region> regions,
      Map<String, String> parameters) {
    cleanup();
    List<CacheEntry> clist = cache.get(id);
    if(clist == null) {
      return null;
    }
    List<CacheEntry> result = Lists.newArrayList();
    for(CacheEntry ce : clist) {
      if(ce.key().matches(id, type, regions, parameters)) {
        result.add(ce);
      }
    }
    return result;
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
}
