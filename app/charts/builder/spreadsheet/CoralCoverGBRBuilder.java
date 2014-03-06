package charts.builder.spreadsheet;

import charts.ChartType;
import charts.Region;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public abstract class CoralCoverGBRBuilder extends CoralCoverBuilder {

  public CoralCoverGBRBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected ADSCDataset createDataset(Context ctx) {
    return ctx.region() == Region.GBR ? super.createDataset(ctx) : null;
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.TITLE, "${type} (mean)").
        put(Attribute.X_AXIS_LABEL, "Region").
        put(Attribute.Y_AXIS_LABEL, COVER).
        build();
  }
}
