package charts.builder.spreadsheet;

import java.util.Map;

import charts.ChartType;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableMap;

public class ChlorophyllABuilder extends MarineBarChartBuilder {

  private static final String TITLE = "Area (%) where the annual mean value for chlorophyll a" +
      " exceeded the Water Quality Guidelines";

  private static final Map<Attribute, Object> DEFAULTS = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE, TITLE,
      Attribute.RANGE_AXIS_LABEL, "Area (%)",
      Attribute.DOMAIN_AXIS_LABEL, "Region");

  public ChlorophyllABuilder() {
    super(ChartType.CHLOROPHYLL_A);
  }

  @Override
  Map<Attribute, Object> defaults() {
    return DEFAULTS;
  }
}
