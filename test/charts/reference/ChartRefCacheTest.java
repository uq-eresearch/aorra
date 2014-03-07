package charts.reference;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import charts.ChartType;

public class ChartRefCacheTest {

  @Test
  public void testRefenceCache() {
    final ChartRefCache cache = new ChartRefCache();
    try {
      cache.start();
      while(!cache.initialized()) {
        try {
          TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {}
      }
      for(ChartType type : ChartType.values()) {
        CacheResult c = cache.cached(type);
        Assert.assertNotNull(String.format("chart type %s missing in reference cache",
            type.name()), c);
      }
    } finally {
      cache.stop();
    }
  }

}
