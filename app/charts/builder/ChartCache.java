package charts.builder;

import java.util.List;

import play.libs.F;
import charts.Chart;

public interface ChartCache {

  public void cleanup(String fileId);

  public abstract F.Either<Exception,List<Chart>> getCharts(String id, DataSourceFactory dsf);

}