package charts.reference;

import java.util.Map;

public class ChartReference {
  private final String type;
  private final String label;
  private final String title;
  private final Map<String, Object> defaults;
  private final Map<String, String> substitutions;

  public ChartReference(String type, String label,
      Map<String, Object> defaults, Map<String, String> substitutions, String title) {
    this.type = type;
    this.label = label;
    this.defaults = defaults;
    this.substitutions = substitutions;
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public String getLabel() {
    return label;
  }

  public String getTitle() {
    return title;
  }

  public Map<String, Object> getDefaults() {
    return defaults;
  }

  public Map<String, String> getSubstitutions() {
    return substitutions;
  }

  
}