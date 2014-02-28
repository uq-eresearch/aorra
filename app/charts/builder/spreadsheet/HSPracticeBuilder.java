package charts.builder.spreadsheet;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.HSLandPracticeSystems;
import charts.graphics.ManagementPracticeSystems;
import charts.jfree.ADCDataset;

public abstract class HSPracticeBuilder extends LandPracticeBuilder {

  private static final String[] ABCD = {"A", "B", "C", "D" };

  private static final String[] PS = { "Nutrients", "Herbicides", "Soil" };

  public HSPracticeBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    return cellEquals(datasource, PS[0], "B2") &&
        cellEquals(datasource, PS[1], "F2") &&
        cellEquals(datasource, PS[2], "J2");
  }

  @Override
  protected Double[] readRow(SpreadsheetDataSource ds, int row) throws MissingDataException {
    Double[] values = new Double[12];
    for(int i=0;i<12;i++) {
      values[i] = ds.select(row, i+1).asPercent();
    }
    return values;
  }

  @Override
  protected void addData(ADCDataset dataset, String year, Double[] values) {
    for(int i=0;i<12;i++) {
      dataset.addValue(values[i], String.format("%s_%s", ABCD[i%4], year), PS[i/4]);
    }
  }

  @Override
  protected ManagementPracticeSystems renderer() {
    return new HSLandPracticeSystems();
  }

}
