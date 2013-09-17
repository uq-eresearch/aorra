package charts.builder;

import java.awt.Dimension;
import java.util.List;

import charts.Chart;
import charts.ChartType;
import charts.Region;

public interface ChartTypeBuilder {

  boolean canHandle(ChartType type, List<DataSource> datasources);

  List<Chart> build(
      List<DataSource> datasources,
      ChartType type,
      List<Region> regions,
      Dimension dimensions);

}
