package charts.jfree;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;

public enum Attribute {

  TITLE("title", String.class),
  TYPE("type", ChartType.class),
  SERIES_COLORS("series colors", Color[].class),
  SERIES_COLOR("series color", Color.class),
  RANGE_AXIS_LABEL("range axis label", String.class, "range axis title", "y-axis label"),
  DOMAIN_AXIS_LABEL("domain axis label", String.class, "domain axis title", "x-axis label");

  private String name;
  private Class<?> type;
  private String[] synonyms;

  private Attribute(String name, Class<?> type, String... synonyms) {
    this.name = name;
    this.type = type;
    this.synonyms = synonyms;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }

  private boolean hasSynonym(String synonym) {
    for(String s : synonyms) {
      if(StringUtils.equalsIgnoreCase(s, synonym)) {
        return true;
      }
    }
    return false;
  }

  public static Attribute lookup(String name) {
    for(Attribute a : Attribute.values()) {
      if(StringUtils.equalsIgnoreCase(a.getName(), name) ||
          a.hasSynonym(name)) {
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
