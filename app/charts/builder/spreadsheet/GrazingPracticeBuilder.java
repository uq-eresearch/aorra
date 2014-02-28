package charts.builder.spreadsheet;

import charts.ChartType;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.GrazingPracticeSystems;
import charts.graphics.ManagementPracticeSystems;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class GrazingPracticeBuilder extends ManagementPracticeBuilder {

  private static final String[] ABCD = {"A", "B", "C", "D" };

  public GrazingPracticeBuilder() {
    super(ChartType.GRAZING_PS);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    return cellEquals(datasource, "A", "B2") &&
        cellEquals(datasource, "B", "C2") &&
        cellEquals(datasource, "C", "D2") &&
        cellEquals(datasource, "D", "E2") &&
        cellEquals(datasource, "Grazing", "A1");
  }

  @Override
  protected Double[] readRow(SpreadsheetDataSource ds, int row) throws MissingDataException {
    Double[] values = new Double[4];
    for(int i=0;i<4;i++) {
      values[i] = ds.select(row, i+1).asPercent();
    }
    return values;
  }

  @Override
  protected void addData(ADCDataset dataset, String year, Double[] values) {
    for(int i=0;i<4;i++) {
      dataset.addValue(values[i], year, ABCD[i]);
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    AttributeMap m = super.defaults(type);
    m.put(Attribute.Y_AXIS_LABEL, "% of graziers");
    return m;
  }

  @Override
  protected ManagementPracticeSystems renderer() {
    return new GrazingPracticeSystems();
  }

}
