package charts.reference;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import charts.ChartType;
import charts.builder.ChartTypeBuilder;
import charts.builder.spreadsheet.AbstractBuilder;
import charts.builder.spreadsheet.ChartConfigurationNotSupported;
import charts.builder.spreadsheet.SubstitutionKey;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ChartRefConfig {

  private final ChartRefCache cache;

  @Inject
  public ChartRefConfig(ChartRefCache cache) {
    this.cache = cache;
  }

  public List<ChartReference> chartrefs() {
    List<ChartReference> result = Lists.newArrayList();
    for(ChartType type : ChartType.values()) {
      AbstractBuilder builder = getChartBuilder(type);
      if(builder != null) {
        result.add(new ChartReference(type.name(), type.getLabel(),
            defaults(builder, type), (defaults(builder, type) != null?substMap(builder):null),
            type.hasUniqueLabel()?type.getLabel():String.format("%s (%s)",
                type.getLabel(), type.name())));
      }
    }
    Collections.sort(result, new Comparator<ChartReference>() {
      @Override
      public int compare(ChartReference cr0, ChartReference cr1) {
        return cr0.getLabel().compareTo(cr1.getLabel());
      }});
    return result;
  }

  private AbstractBuilder getChartBuilder(ChartType type) {
    for(ChartTypeBuilder builder : cache.builder().builders()) {
      if(builder instanceof AbstractBuilder) {
        AbstractBuilder a = (AbstractBuilder)builder;
        if(a.supports(type)) {
          return a;
        }
      }
    }
    return null;
  }

  private Map<String, Object> defaults(AbstractBuilder builder, ChartType type) {
    try {
      return asMap(builder.defaults(type));
    } catch(ChartConfigurationNotSupported e) {
      return null;
    }
  }

  private Map<String, Object> asMap(AttributeMap map) {
    if(map == null) {
      return null;
    }
    Map<String, Object> m = Maps.newTreeMap();
    for(Attribute<?> a : map.keySet()) {
      Object o = map.get(a);
      if(o!=null) {
        if(o instanceof Color) {
          m.put(a.getName(), new Colors((Color)o));
        } else if(o instanceof Color[]) {
          m.put(a.getName(), new Colors((Color[])o));
        } else {
          m.put(a.getName(), o.toString());
        }
      } else {
        m.put(a.getName(), null);
      }
    }
    return m;
  }

  private Map<String, String> substMap(AbstractBuilder builder) {
    Map<String, String> m = Maps.newTreeMap();
    Set<SubstitutionKey> set = builder.substitutionKeys();
    for(SubstitutionKey s : set) {
      m.put(s.getName(), s.getDescription());
    }
    return m;
  }

}
