package charts.builder;

import java.util.List;

import charts.Chart;

public interface ChartCache {

  public abstract List<Chart> getCharts(String id, DataSourceFactory dsf);

}