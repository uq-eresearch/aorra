package charts.builder.spreadsheet;

import charts.ChartType;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class CoralCoverJuvGBRBuilder extends CoralCoverGBRBuilder {

  public CoralCoverJuvGBRBuilder() {
    super(ChartType.CORAL_JUV_GBR);
  }

  @Override
  protected String getMeanColumn() {
    return JUV_DEN;
  }

  @Override
  protected String getSeColumn() {
    return JUV_DEN_SE;
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.Y_AXIS_LABEL, JUVENILE).
        put(Attribute.Y_AXIS_RANGE, "10").
        put(Attribute.Y_AXIS_TICKS, "1").
        build();
  }

}
