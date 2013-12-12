package charts.builder;

import java.util.Collections;
import java.util.List;

import notification.Notifier;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.libs.F;
import service.EventManager;
import service.OrderedEvent;
import service.EventManager.EventReceiver;
import service.EventManager.EventReceiverMessage;
import akka.actor.TypedActor;
import charts.Chart;
import charts.Region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;

public class ChartCacheImpl implements ChartCache {

  private final DefaultChartBuilder chartBuilder;

  private final Cache<String, List<Chart>> cache = CacheBuilder
      .newBuilder()
      .maximumSize(maxsize())
      .removalListener(new RemovalListener<String, List<Chart>>() {
        @Override
        public void onRemoval(RemovalNotification<String, List<Chart>> entry) {
          Logger.debug(String.format("Removing %s charts for id %s from cache",
              entry.getValue().size(), entry.getKey()));
        }})
      .build();

  public ChartCacheImpl(DefaultChartBuilder chartBuilder, EventManager eventManager) {
    this.chartBuilder = chartBuilder;
    final ChartCache cc = TypedActor.<ChartCache>self();
    final EventReceiver er = new EventReceiver() {
      @Override
      public void end() {}

      @Override
      public void end(Throwable e) {}

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

  private int maxsize() {
    return Play.application().configuration().getInt("application.ccmaxsize", 100);
  }

  @Override
  public void cleanup(String fileId) {
    cache.invalidate(fileId);
  }

  @Override
  public F.Either<Exception, List<Chart>> getCharts(String id, DataSourceFactory dsf) {
    try {
      List<Chart> clist = cache.getIfPresent(id);
      if(clist == null) {
        clist = actuallyGetCharts(id, dsf);
        cache.put(id, clist);
      }
      return F.Either.Right(clist);
    } catch (Exception e) {
      return F.Either.Left(e);
    }
  }

  private List<Chart> actuallyGetCharts(String id, DataSourceFactory dsf) throws Exception {
    return chartBuilder.getCharts(id, dsf, null, Collections.<Region>emptyList(), null);
  }
}
