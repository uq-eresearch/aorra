package charts.jfree;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class AttributeMap {

  public static class Builder {
    private AttributeMap m = new AttributeMap();
    public <T> Builder put(Attribute<T> key, T value) {
      m.put(key, value);
      return this;
    }
    public AttributeMap build() {
      return m;
    }
  }

  private Map<Attribute<?>, Object> m = Maps.newHashMap();

  public <T> T get(Attribute<T> key) {
    return key.getType().cast(m.get(key));
  }

  public <T> void put(Attribute<T> key, T value) {
    m.put(key, value);
  }

  public void putAll(AttributeMap map) {
    if(map != null) {
      m.putAll(map.m);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> Set<Attribute<T>> getKeys(Class<T> type) {
    Set<Attribute<T>> result = Sets.newHashSet();
    for(Attribute<?> a : m.keySet()) {
      if(a.getType().isAssignableFrom(type)) {
        result.add((Attribute<T>)a);
      }
    }
    return result;
  }

  public Set<Attribute<?>> keySet() {
    return m.keySet();
  }
}
