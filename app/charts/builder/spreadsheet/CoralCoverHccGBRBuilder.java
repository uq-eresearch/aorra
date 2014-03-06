package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverHccGBRBuilder extends CoralCoverGBRBuilder {

  public CoralCoverHccGBRBuilder() {
    super(ChartType.CORAL_HCC_GBR);
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
