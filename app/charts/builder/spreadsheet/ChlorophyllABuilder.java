package charts.builder.spreadsheet;

import charts.ChartType;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class ChlorophyllABuilder extends MarineBarChartBuilder {

  private static final String TITLE = "Area (%) where the annual mean value for chlorophyll a" +
      " exceeded the Water Quality Guidelines";

  public ChlorophyllABuilder() {
    super(ChartType.CHLOROPHYLL_A);
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, TITLE).
        put(Attribute.Y_AXIS_LABEL, "Area (%)").
        put(Attribute.X_AXIS_LABEL, "Region").
        build();
  }
}
