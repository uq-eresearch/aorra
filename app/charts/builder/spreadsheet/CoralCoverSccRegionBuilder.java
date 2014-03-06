package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverSccRegionBuilder extends CoralCoverRegionBuilder {

  public CoralCoverSccRegionBuilder() {
    super(ChartType.CORAL_SCC);
  }

  @Override
  protected String getMeanColumn() {
    return SC_MEAN;
  }

  @Override
  protected String getSeColumn() {
    return SC_SE;
  }
}
