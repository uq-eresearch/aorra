package charts.builder;

import java.util.Collections;
import java.util.List;

import play.Logger;
import play.Play;
import play.libs.F;
import scala.concurrent.Future;
import service.EventManager;
import service.EventManager.EventReceiver;
import service.EventManager.EventReceiverMessage;
import service.OrderedEvent;
import akka.actor.TypedActor;
import charts.Chart;
import charts.Region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

public class ChartCacheImpl implements ChartCache {

  private final DefaultChartBuilder chartBuilder;

  private final Cache<String, List<Chart>> cache = CacheBuilder.newBuilder()
      .maximumWeight(maxSize())
      .weigher(new Weigher<String, List<Chart>>() {
        @Override
        public int weigh(String k, List<Chart> charts) {
          return charts.size();
        }
      })
      .removalListener(new RemovalListener<String, List<Chart>>() {
        @Override
        public void onRemoval(RemovalNotification<String, List<Chart>> entry) {
          Logger.debug(String.format("Removing %s charts for id %s from cache",
              entry.getValue().size(), entry.getKey()));
        }
      })
      .build();

  public ChartCacheImpl(DefaultChartBuilder chartBuilder,
      EventManager eventManager) {
    this.chartBuilder = chartBuilder;
    final ChartCache cc = TypedActor.<ChartCache> self();
    final EventReceiver er = new EventReceiver() {
      @Override
      public void end() {
      }

      @Override
      public void end(Throwable e) {
      }

      @Override
      public void push(OrderedEvent oe) {
        if (oe.event().type.startsWith("file:")) {
          // Trigger notification for event
          cc.cleanup(oe.event().info("id"));
        }
      }
    };
    eventManager.tell(EventReceiverMessage.add(er, null));
  }

  private int maxSize() {
    return Play.application().configuration()
        .getInt("application.ccmaxsize", 100);
  }

  @Override
  public void cleanup(String fileId) {
    cache.invalidate(fileId);
  }

  @Override
  public Future<List<Chart>> getCharts(String id,
      DataSourceFactory dsf) {
    F.Promise<List<Chart>> promisedCharts;
    try {
      List<Chart> clist = cache.getIfPresent(id);
      if (clist == null) {
        clist = actuallyGetCharts(id, dsf);
        cache.put(id, clist);
      }
      promisedCharts = F.Promise.pure(clist);
    } catch (Exception e) {
      promisedCharts = F.Promise.throwing(e);
    }
    return promisedCharts.wrapped();
  }

  private List<Chart> actuallyGetCharts(String id, DataSourceFactory dsf)
      throws Exception {
    return chartBuilder.getCharts(id, dsf, null,
        Collections.<Region> emptyList(), null);
  }
}
