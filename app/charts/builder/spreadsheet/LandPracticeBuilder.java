package charts.builder.spreadsheet;

import java.util.Map;

import com.google.common.collect.Maps;

import charts.ChartType;
import charts.Region;
import charts.jfree.Attribute;

public abstract class LandPracticeBuilder extends ManagementPracticeBuilder {

  public LandPracticeBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected Map<Attribute, Object> defaults(SpreadsheetDataSource ds, Region region) {
    Map<Attribute, Object> result = Maps.newHashMap();
    result.putAll(super.defaults(ds, region));
    result.put(Attribute.RANGE_AXIS_TITLE, "% of landholders");
    return result;
  }
}
