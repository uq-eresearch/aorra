package charts.builder.spreadsheet;

import charts.ChartType;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class TotalSuspendedSedimentBuilder extends MarineBarChartBuilder {

  public TotalSuspendedSedimentBuilder() {
    super(ChartType.TOTAL_SUSPENDED_SEDIMENT);
  }

  @Override
  protected AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE,"Area (%) where the annual mean value for total suspended solids" +
            " exceeded the Water Quality Guidelines").
            put(Attribute.RANGE_AXIS_LABEL, "Area (%)").
            put(Attribute.DOMAIN_AXIS_LABEL, "Region").
            build();
  }

}
