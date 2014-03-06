package charts.builder.spreadsheet;

import charts.ChartType;
import charts.Region;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public abstract class CoralCoverRegionBuilder extends CoralCoverBuilder {
  
  public CoralCoverRegionBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected ADSCDataset createDataset(Context ctx) {
    return ctx.region() != Region.GBR ? super.createDataset(ctx) : null;
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.TITLE, "${type} (mean) in the ${region} region").
        put(Attribute.X_AXIS_LABEL, "Year").
        put(Attribute.Y_AXIS_LABEL, COVER).
        build();
  }
}
