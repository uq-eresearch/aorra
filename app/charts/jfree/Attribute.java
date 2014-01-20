package charts.jfree;

import java.awt.Color;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import charts.Region;

import com.google.common.collect.Lists;

public class Attribute<T> {

  private String name;
  private Class<T> type;

  private static final List<Attribute<?>> ATTRIBUTES = Lists.newArrayList();

  public static final Attribute<String> TITLE = of("title", String.class);
  public static final Attribute<Region> REGION = of("region", Region.class);
  public static final Attribute<Color[]> SERIES_COLORS = of("seriesColors", Color[].class);
  public static final Attribute<Color> SERIES_COLOR = of("seriesColor", Color.class);
  public static final Attribute<String> RANGE_AXIS_TITLE = of("range axis title", String.class);

  private Attribute(String name, Class<T> type) {
    this.name = name;
    this.type = type;
    ATTRIBUTES.add(this);
  }

  public String getName() {
    return name;
  }

  public Class<T> getType() {
    return type;
  }

  public static Attribute<?> lookup(String name) {
    for(Attribute<?> a : ATTRIBUTES) {
      if(StringUtils.equalsIgnoreCase(a.getName(), name)) {
        return a;
      }
    }
    return null;
  }

  private static <T> Attribute<T> of(String name, Class<T> type) {
    return new Attribute<T>(name, type);
  }

  @Override
  public String toString() {
    return String.format("%s/%s", getName(), getType().getSimpleName());
  }

}
