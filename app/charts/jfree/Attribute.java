package charts.jfree;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.Region;

public enum Attribute {

  TITLE("title", String.class),
  TYPE("type", ChartType.class),
  REGION("region", Region.class),
  SERIES_COLORS("seriesColors", Color[].class),
  SERIES_COLOR("seriesColor", Color.class),
  RANGE_AXIS_TITLE("range axis title", String.class),
  DOMAIN_AXIS_TITLE("domain axis title", String.class);

  private String name;
  private Class<?> type;

  private Attribute(String name, Class<?> type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  public static Attribute lookup(String name) {
    for(Attribute a : Attribute.values()) {
      if(StringUtils.equalsIgnoreCase(a.getName(), name)) {
        return a;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return String.format("%s/%s", getName(), getType().getSimpleName());
  }

}
