package charts.builder.spreadsheet;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.GrainsPracticeSystems;
import charts.graphics.ManagementPracticeSystems;
import charts.jfree.ADCDataset;

public class GrainsPracticeBuilder extends LandPracticeBuilder {

  private static final String[] ABCD = {"A", "B", "C/D" };

  private static final String[] PS = { "Nutrients", "Herbicides", "Soil" };

  public GrainsPracticeBuilder() {
    super(ChartType.GRAINS_PS);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    return cellEquals(datasource, PS[0], "B2") &&
        cellEquals(datasource, PS[1], "E2") &&
        cellEquals(datasource, PS[2], "H2") && 
        cellEquals(datasource, "Grains", "A1");
  }

  @Override
  protected Double[] readRow(SpreadsheetDataSource ds, int row) throws MissingDataException {
    Double[] values = new Double[9];
    for(int i=0;i<9;i++) {
      values[i] = ds.select(row, i+1).asPercent();
    }
    return values;
  }

  @Override
  protected void addData(ADCDataset dataset, String year, Double[] values) {
    for(int i=0;i<9;i++) {
      dataset.addValue(values[i], String.format("%s_%s", ABCD[i%3], year), PS[i/3]);
    }
  }

  @Override
  protected ManagementPracticeSystems renderer() {
    return new GrainsPracticeSystems();
  }

}
