package charts.builder.spreadsheet;

import charts.ChartType;

public class CoralCoverSccGBRBuilder extends CoralCoverGBRBuilder {

  public CoralCoverSccGBRBuilder() {
    super(ChartType.CORAL_SCC_GBR);
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
