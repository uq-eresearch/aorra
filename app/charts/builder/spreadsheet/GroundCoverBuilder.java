package charts.builder.spreadsheet;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class GroundCoverBuilder extends AbstractGroundCoverBuilder {

  public GroundCoverBuilder() {
    super(ChartType.GROUNDCOVER);
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
        put(Attribute.TITLE, "Mean late dry season groundcover in the ${gcRegion}"
            + " for ${firstYear}-${lastYear}").
        put(Attribute.Y_AXIS_LABEL, "Groundcover (%)").
        build();
  }

}
