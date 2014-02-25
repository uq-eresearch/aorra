package charts.builder.spreadsheet;

import charts.builder.ChartTypeBuilder;

public class ChartConfigurationNotSupported extends RuntimeException {

  private ChartTypeBuilder builder;

  public ChartConfigurationNotSupported(ChartTypeBuilder builder) {
    this.builder = builder;
  }

  @Override
  public String getMessage() {
    return String.format("chart builder %s does not support chart configuration yet",
        builder.getClass().getName());
  }
}
