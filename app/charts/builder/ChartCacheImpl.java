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

  @Override
  public void cleanup(String fileId) {
    cache.invalidate(fileId);
  }

  @Override
  public Future<List<Chart>> getCharts(final String id) {
    // Check the cache
    final List<Chart> clist = cache.getIfPresent(id);
    final F.Promise<List<Chart>> promisedCharts;
    if (clist == null) {
      // Charts will need to be created, so get promise to be fulfilled
      promisedCharts = getPromiseOfCharts(id);
    } else {
      // Create a promise with the value already fulfilled
      promisedCharts = F.Promise.pure(clist);
    }
    // Return back a Scala Future (required by Akka for detecting async)
    // that will eventually provide the charts.
    return promisedCharts.wrapped();
  }

  @Override
  public void update(String fileId, List<Chart> charts) {
    cache.put(fileId, charts);
  }

  private List<Chart> actuallyGetCharts(String id) throws Exception {
    return chartBuilder.getCharts(id, null,
        Collections.<Region>emptyList(), null);
  }

  private F.Promise<List<Chart>> getPromiseOfCharts(final String id) {
    // Get reference to ourself, so we can update the cache asynchronously
    final ChartCache self = TypedActor.self();
    // Create a promise based on an asynchronous operation
    return F.Promise.promise(new F.Function0<List<Chart>>() {
      @Override
      public List<Chart> apply() throws Throwable {
        // Get the charts (non-modifying operation)
        final List<Chart> charts = actuallyGetCharts(id);
        // Schedule update of self with new cache value
        self.update(id, charts);
        // Fulfil promise with charts
        return charts;
      }
    }, TypedActor.dispatcher()); // Execute on our own thread-pool
  }

  private int maxSize() {
    return Play.application().configuration()
        .getInt("application.chartCache.size", 100);
  }
}
