package charts.builder.spreadsheet;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public abstract class GroundCoverBuilder extends AbstractGroundCoverBuilder {

  public GroundCoverBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    if(StringUtils.equalsIgnoreCase(ctx.datasource().getDefaultSheet(), GC_SHEETNAME)) {
      return super.createDataset(ctx);
    } else {
      return null;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.Y_AXIS_LABEL, "Groundcover (%)").
        build();
  }

}
