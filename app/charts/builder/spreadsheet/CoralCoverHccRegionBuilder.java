package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverHccRegionBuilder extends CoralCoverRegionBuilder {

  public CoralCoverHccRegionBuilder() {
    super(ChartType.CORAL_HCC);
  }

  @Override
  protected String getMeanColumn() {
    return HC_MEAN;
  }

  @Override
  protected String getSeColumn() {
    return HC_SE;
  }
}
