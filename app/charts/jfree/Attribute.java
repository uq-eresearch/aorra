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
  public static final Attribute<Color> SERIES_COLOR = colorAttr("series color");
  public static final Attribute<Color[]> SERIES_COLORS = attr("series colors", Color[].class);
  public static final Attribute<Color> CONDITION_NOT_EVALUATED = colorAttr("not evaluated");
  public static final Attribute<Color> CONDITION_VERY_GOOD = colorAttr("very good");
  public static final Attribute<Color> CONDITION_GOOD = colorAttr("good");
  public static final Attribute<Color> CONDITION_MODERATE = colorAttr("moderate");
  public static final Attribute<Color> CONDITION_POOR = colorAttr("poor");
  public static final Attribute<Color> CONDITION_VERY_POOR = colorAttr("very poor");

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

  private static Attribute<Color> colorAttr(String label) {
    return attr(label, Color.class);
  }

  private static <T> Attribute<T> attr(String label, Class<T> type) {
    return attr(label, type, (String[])null);
  }

  private static <T> Attribute<T> attr(String label, Class<T> type, String... synonyms) {
    return new Attribute<>(label, type, synonyms);
  }

}
