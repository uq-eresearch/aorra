package charts.builder.spreadsheet;

import charts.ChartType;
import charts.Region;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class GroundCoverBelow50GBRBuilder extends GroundCoverBelow50Builder {

  public GroundCoverBelow50GBRBuilder() {
    super(ChartType.GROUNDCOVER_BELOW_50_GBR);
  }

  @Override
  protected ADCDataset createDataset(Context context) {
    if(context.region() == Region.GBR) {
      return super.createDataset(context);
    } else {
      return null;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.TITLE, TITLE_START
            + "Great Barrier Reef catchment and regions for ${firstYear}-${lastYear}").
        build();
  }
}
