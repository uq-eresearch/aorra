package charts.builder;

import java.util.List;
import java.util.Map;

import charts.Chart;
import charts.ChartType;
import charts.Region;

public interface ChartBuilder {

  public List<Chart> getCharts(String id, DataSourceFactory dsf, ChartType type,
      List<Region> regions, Map<String, String> parameters) throws Exception;

}
