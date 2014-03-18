package controllers;

import java.io.InputStream;

import org.jcrom.Jcrom;

import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import charts.ChartType;
import charts.reference.CacheResult;
import charts.reference.ChartRefCache;
import charts.reference.ChartRefConfig;

import com.google.inject.Inject;

@With(UncacheableAction.class)
public class ChartReferenceController extends SessionAwareController {

  private final ChartRefConfig config;

  private final ChartRefCache cache;

  @Inject
  public ChartReferenceController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStoreImpl,
      final FlagStore flagStoreImpl,
      final ChartRefConfig config,
      final ChartRefCache cache) {
    super(sessionFactory, jcrom, sessionHandler);
    this.config = config;
    this.cache = cache;
  }

  @SubjectPresent
  public Result index() {
    return ok(views.html.Application.chartref.render()).as("text/html; charset=utf-8");
  }

  @SubjectPresent
  public Result config() {
    return ok(Json.toJson(config.chartrefs())).as("application/json; charset=utf-8");
  }

  @SubjectPresent
  public Result chart(final String chartType) {
    final ChartType type;
    try {
      type = ChartType.getChartType(chartType);
    } catch (IllegalArgumentException e) {
      return notFound("unknown chart type " + chartType);
    }
    CacheResult r = cache.cached(type);
    if(r != null) {
      try {
        if(!modified(r.created())) {
          return status(NOT_MODIFIED);
        }
      } catch(Exception e) {}
      InputStream content  = r.content();
      if(content != null) {
        ctx().response().setHeader("Last-Modified", asHttpDate(r.created()));
        return ok(content).as("image/svg+xml; charset=utf-8");
      } else {
        return internalServerError();
      }
    } else {
      return notFound("not in cache");
    }
  }

  @SubjectPresent
  public Result datasource(final String chartType) {
    final ChartType type;
    try {
      type = ChartType.getChartType(chartType);
    } catch (IllegalArgumentException e) {
      return notFound("unknown chart type " + chartType);
    }
    CacheResult r = cache.cached(type);
    if(r != null) {
      ctx().response().setHeader("Content-Disposition",
          String.format("attachment; filename=%s.%s",
              type.name().toLowerCase(), r.datasourceExtension()));
      return ok(r.datasource()).as(r.datasourceMimetype());
    } else {
      return notFound("not in cache");
    }
  }

}
