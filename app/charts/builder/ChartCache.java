package charts.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Region;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
        //System.out.println("match id failed");
        return false;
      }
      if(type != null && type != this.type) {
        //System.out.println("match type failed");
        return false;
      }
      if(regions != null && !regions.isEmpty() && !regions.contains(region)) {
        //System.out.println("match region failed");
        return false;
      }
      if(parameters != null && this.parameters != null) {
        for(Map.Entry<String, String> me: parameters.entrySet()) {
          if(!StringUtils.equals(me.getValue(), this.parameters.get(me.getKey()))) {
            //System.out.println("match parameters failed on "+me.getKey());
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

  public synchronized void noCharts(String id) {
    add(id, null);
  }

  public synchronized void add(String id, Chart chart) {
    List<CacheEntry> clist = cache.get(id);
    if(clist == null) {
      System.out.println("adding clist for "+id);
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
          System.out.println("removing duplicate");
          ceIter.remove();
        }
      }
      System.out.println("adding "+k.toString());
      clist.add(new CacheEntry(k, chart));
      System.out.println("clist size "+clist.size());
    }
  }

  public synchronized void invalidate(String id) {
    System.out.println("invalidate "+id);
    //TODO
  }

  public synchronized List<CacheEntry> get(String id, ChartType type, List<Region> regions,
      Map<String, String> parameters) {
    List<CacheEntry> clist = cache.get(id);
    if(clist == null) {
      System.out.println("XXX cache miss");
      return null;
    }
    List<CacheEntry> result = Lists.newArrayList();
    System.out.println("XXX cache hit, found clist for "+id);
    for(CacheEntry ce : clist) {
      if(ce.key().matches(id, type, regions, parameters)) {
        result.add(ce);
      }
    }
    return result;
  }

}
