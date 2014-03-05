package charts.jfree;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

public class Attribute<T> {

  private static final List<Attribute<?>> ATTRIBUTES = Lists.newArrayList();

  public static final Attribute<String> TITLE = strAttr("title");
  public static final Attribute<String> Y_AXIS_LABEL = strAttr("y-axis label",
      "range axis label", "range axis title");
  public static final Attribute<String> X_AXIS_LABEL = strAttr("x-axis label",
      "domain axis label", "domain axis title");
  public static final Attribute<Color> SERIES_COLOR = attr("series color", Color.class);
  public static final Attribute<Color[]> SERIES_COLORS = attr("series colors", Color[].class);

  private final String name;
  private final Class<T> type;
  private final String[] synonyms;

  private Attribute(String name, Class<T> type, String... synonyms) {
    this.name = name;
    this.type = type;
    this.synonyms = synonyms;
    ATTRIBUTES.add(this);
  }

  public String getName() {
    return name;
  }

  public Class<T> getType() {
    return type;
  }

  private boolean hasSynonym(String synonym) {
    if(synonyms != null) {
      for(String s : synonyms) {
        if(StringUtils.equalsIgnoreCase(s, synonym)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s/%s", getName(), getType().getSimpleName());
  }

  public static Attribute<?> lookup(String name) {
    for(Attribute<?> a : Attribute.values()) {
      if(StringUtils.equalsIgnoreCase(a.getName(), name) ||
          a.hasSynonym(name)) {
        return a;
      }
    }
    return null;
  }

  public static List<Attribute<?>> values() {
    return Collections.unmodifiableList(ATTRIBUTES);
  }

  private static Attribute<String> strAttr(String label) {
    return attr(label, String.class);
  }

  private static Attribute<String> strAttr(String label, String... synonyms) {
    return attr(label, String.class, synonyms);
  }

  private static <T> Attribute<T> attr(String label, Class<T> type) {
    return attr(label, type, (String[])null);
  }

  private static <T> Attribute<T> attr(String label, Class<T> type, String... synonyms) {
    return new Attribute<>(label, type, synonyms);
  }

}
