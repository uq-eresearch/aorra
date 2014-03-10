package charts.builder.spreadsheet;

import charts.ChartType;
import charts.Region;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class GroundCoverRegionBuilder extends GroundCoverBuilder {

  public GroundCoverRegionBuilder() {
    super(ChartType.GROUNDCOVER);
  }

  @Override
  protected ADCDataset createDataset(Context context) {
    if(context.region() != Region.GBR) {
      return super.createDataset(context);
    } else {
      return null;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.TITLE, "Mean late dry season groundcover in the ${region}"
            + " catchment for ${firstYear}-${lastYear}").
        build();
  }
}
