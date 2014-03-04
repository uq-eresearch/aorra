package charts.builder.spreadsheet;

import java.util.Map;

import org.jfree.data.general.Dataset;

import charts.ChartType;
import charts.Region;

public class JFreeContext extends Context {
  private final Dataset dataset;
  public JFreeContext(SpreadsheetDataSource datasource, ChartType type,
      Region region, Map<String, String> parameters, Dataset dataset) {
    super(datasource, type, region, parameters);
    this.dataset = dataset;
  }
  public Dataset dataset() {
    return dataset;
  }
}