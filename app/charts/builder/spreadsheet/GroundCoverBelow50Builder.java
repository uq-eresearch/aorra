package charts.builder.spreadsheet;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class GroundCoverBelow50Builder extends AbstractGroundCoverBuilder {

  protected static final String TITLE_START =
      "Percentage of the reporting area with groundcover below 50 per cent in the ";

  public GroundCoverBelow50Builder() {
    super(ChartType.GROUNDCOVER_BELOW_50);
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    if(StringUtils.equalsIgnoreCase(ctx.datasource().getDefaultSheet(), GCB50_SHEETNAME)) {
      return super.createDataset(ctx);
    } else {
      return null;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.Y_AXIS_LABEL, "Area (%)").
        put(Attribute.TITLE, TITLE_START
            + "${gcRegion} for ${firstYear}-${lastYear}").
        build();
  }
}
