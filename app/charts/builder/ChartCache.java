package charts.builder;

import java.util.List;

import charts.Chart;
import scala.concurrent.Future;

public interface ChartCache {

  public void cleanup(String fileId);

  public Future<List<Chart>> getCharts(String id);

  public void update(String fileId, List<Chart> charts);

}