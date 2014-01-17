package charts.builder.spreadsheet;

import charts.ChartType;

public class SPracticeBuilder extends HSPracticeBuilder {

  public SPracticeBuilder() {
    super(ChartType.SUGARCANE_PS);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    return super.canHandle(datasource) && cellEquals(datasource, "Sugarcane", "A1");
  }

}
