package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jcrom.Jcrom;

import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import charts.ChartType;
import charts.builder.ChartTypeBuilder;
import charts.builder.DefaultChartBuilder;
import charts.builder.spreadsheet.AbstractBuilder;
import charts.builder.spreadsheet.ChartConfigurationNotSupported;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class ChartReferenceController extends SessionAwareController {
  
  public static class ChartReference {
    public String type;
    public String label;
    public Map<String, String> defaults;
    public ChartReference(String type, String label, Map<String, String> defaults) {
      this.type = type;
      this.label = label;
      this.defaults = defaults;
    }
  }

  private final DefaultChartBuilder chartBuilder;

  @Inject
  public ChartReferenceController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStoreImpl,
      final FlagStore flagStoreImpl,
      final DefaultChartBuilder chartBuilder) {
    super(sessionFactory, jcrom, sessionHandler);
    this.chartBuilder = chartBuilder;
  }

  @SubjectPresent
  public Result index() {
    return ok(views.html.Application.chartref.render()).as("text/html; charset=utf-8");
  }

  @SubjectPresent
  public Result config() {
    return ok(Json.toJson(cfg())).as("application/json; charset=utf-8");
  }

  private List<ChartReference> cfg() {
    List<ChartReference> result = Lists.newArrayList();
    for(ChartType type : ChartType.values()) {
      AbstractBuilder builder = getChartBuilder(type);
      if(builder != null) {
        result.add(new ChartReference(type.name(), type.getLabel(), defaults(builder, type)));
      }
    }
    Collections.sort(result, new Comparator<ChartReference>() {
      @Override
      public int compare(ChartReference cr0, ChartReference cr1) {
        return cr0.label.compareTo(cr1.label);
      }});
    return result;
  }

  private AbstractBuilder getChartBuilder(ChartType type) {
    for(ChartTypeBuilder builder : chartBuilder.builders()) {
      if(builder instanceof AbstractBuilder) {
        AbstractBuilder a = (AbstractBuilder)builder;
        if(a.supports(type)) {
          return a;
        }
      }
    }
    return null;
  }

  private Map<String, String> defaults(AbstractBuilder builder, ChartType type) {
    try {
      return asMap(builder.defaults(type));
    } catch(ChartConfigurationNotSupported e) {
      return null;
    }
  }

  private Map<String, String> asMap(AttributeMap map) {
    if(map == null) {
      return null;
    }
    Map<String, String> m = Maps.newTreeMap();
    for(Attribute<?> a : map.keySet()) {
      Object o = map.get(a);
      if(o!=null) {
        m.put(a.getName(), o.toString());
      } else {
        m.put(a.getName(), null);
      }
    }
    return m;
  }
}
