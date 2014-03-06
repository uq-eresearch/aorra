package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverMaGBRBuilder extends CoralCoverGBRBuilder {
  public CoralCoverMaGBRBuilder() {
    super(ChartType.CORAL_MA_GBR);
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
