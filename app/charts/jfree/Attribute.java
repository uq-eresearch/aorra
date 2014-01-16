package charts.jfree;

import java.awt.Color;


import charts.Region;

public class Attribute<T> {

  private String name;
  private Class<T> type;

  public static final Attribute<String> TITLE = of("title", String.class);
  public static final Attribute<Region> REGION = of("region", Region.class);
  public static final Attribute<Color[]> SERIES_COLORS = of("seriesColors", Color[].class);
  public static final Attribute<Color> SERIES_COLOR = of("seriesColor", Color.class);

  private Attribute(String name, Class<T> type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public Class<T> getType() {
    return type;
  }

  private static <T> Attribute<T> of(String name, Class<T> type) {
    return new Attribute<T>(name, type);
  }

}
