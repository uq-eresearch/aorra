package charts.builder;

import java.util.List;
import java.util.Map;

import charts.Chart;
import charts.ChartType;
import charts.Region;

public interface ChartTypeBuilder {

  List<Chart> build(
      DataSource datasource,
      ChartType type,
      List<Region> regions,
      Map<String, String> parameters);

  Map<String, List<String>> getParameters(DataSource datasource, ChartType type);
}
