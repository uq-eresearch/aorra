package charts.builder.spreadsheet;

import charts.ChartType;

public class HPracticeBuilder extends HSPracticeBuilder {

  public HPracticeBuilder() {
    super(ChartType.HORTICULTURE_PS);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    return super.canHandle(datasource) && cellEquals(datasource, "Horticulture", "A1");
  }
}
