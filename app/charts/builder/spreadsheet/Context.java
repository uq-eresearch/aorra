package charts.builder.spreadsheet;

import java.util.Map;

import charts.ChartType;
import charts.Region;

public class Context {
  private final SpreadsheetDataSource datasource;
  private final ChartType type;
  private final Region region;
  private final Map<String, String> parameters;
  public Context(SpreadsheetDataSource datasource, ChartType type,
      Region region, Map<String, String> parameters) {
    this.datasource = datasource;
    this.type = type;
    this.region = region;
    this.parameters = parameters;
  }
  public ChartType type() {
    return type;
  }
  public Region region() {
    return region;
  }
  public SpreadsheetDataSource datasource() {
    return datasource;
  }
  public Map<String, String> parameters() {
    return parameters;
  }
  @Override
  public String toString() {
    return String.format("%s - %s - %s", type, region, parameters);
  }
}