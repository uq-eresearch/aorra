package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverMaRegionBuilder extends CoralCoverRegionBuilder {

  public CoralCoverMaRegionBuilder() {
    super(ChartType.CORAL_MA);
  }

  @Override
  protected String getMeanColumn() {
    return MA_MEAN;
  }

  @Override
  protected String getSeColumn() {
    return MA_SE;
  }
}
