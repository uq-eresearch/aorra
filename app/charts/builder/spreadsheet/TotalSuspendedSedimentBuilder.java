package charts.builder.spreadsheet;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import charts.ChartType;
import charts.jfree.Attribute;

public class TotalSuspendedSedimentBuilder extends MarineBarChartBuilder {

  private static final Map<Attribute, Object> DEFAULTS = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE,"Area (%) where the annual mean value for total suspended solids" +
          " exceeded the Water Quality Guidelines",
      Attribute.RANGE_AXIS_LABEL, "Area (%)",
      Attribute.DOMAIN_AXIS_LABEL, "Region");

  public TotalSuspendedSedimentBuilder() {
    super(ChartType.TOTAL_SUSPENDED_SEDIMENT);
  }

  @Override
  Map<Attribute, Object> defaults() {
    return DEFAULTS;
  }

}
