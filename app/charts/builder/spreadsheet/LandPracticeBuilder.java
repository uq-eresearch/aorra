package charts.builder.spreadsheet;

import charts.ChartType;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public abstract class LandPracticeBuilder extends ManagementPracticeBuilder {

  public LandPracticeBuilder(ChartType type) {
    super(type);
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    AttributeMap m = super.defaults(type);
    m.put(Attribute.Y_AXIS_LABEL, "% of landholders");
    return m;
  }
}
