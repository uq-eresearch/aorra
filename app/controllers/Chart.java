package controllers;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.getFirst;
import helpers.FileStoreHelper;

import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jcrom.Jcrom;

import play.libs.F;
import play.libs.Json;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import charts.ChartDescription;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartBuilder;
import charts.representations.Format;
import charts.representations.Representation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Chart extends SessionAwareController {

  private final FileStore fileStore;

  private final ChartBuilder chartBuilder;

  @Inject
  public Chart(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
      final CacheableUserProvider sessionHandler, final FileStore fileStore,
      final ChartBuilder chartBuilder) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStore = fileStore;
    this.chartBuilder = chartBuilder;
  }

  @SubjectPresent
  public Result charts(final String format, final String id) throws Exception {
    if (!fileIsAccessible(id)) {
      return notFound();
    }
    final List<charts.Chart> charts =
      chartBuilder.getCharts(id, null,
          getRegions(request().queryString()), null);
    final ObjectNode json = Json.newObject();
    final ArrayNode aNode = json.putArray("charts");
    for (charts.Chart chart : charts) {
      ChartDescription desc = chart.getDescription();
      final ObjectNode chartNode = Json.newObject();
      chartNode.put("type", desc.getType().getLabel());
      chartNode.put("region", desc.getRegion().getName());
      chartNode.put("title", desc.getTitle());
      for (String pname : desc.getParameterNames()) {
        chartNode.put(pname, desc.getParameter(pname).toString());
      }
      try {
        chartNode.put("url", buildUrl(chart, format, id));
      } catch (UnsupportedEncodingException e) {
        // Not going to happen
        throw new RuntimeException(e);
      }
      aNode.add(chartNode);
    }
    return ok(json).as("application/json; charset=utf-8");
  }

  private Map<String, String> getParameters() {
      Map<String, String> parameters = Maps.newHashMap();
      for(Map.Entry<String, String[]> me : request().queryString().entrySet()) {
        if(me.getValue() != null && me.getValue().length > 0) {
          parameters.put(me.getKey(), me.getValue()[0]);
        }
      }
      return parameters;
  }

  @SubjectPresent
  public Result chart(final String chartType,
      final String formatStr, final String id) throws Exception {
    if (!fileIsAccessible(id)) {
      return notFound();
    }
    final ChartType type;
    final Format format;
    try {
      type = ChartType.getChartType(chartType);
    } catch (IllegalArgumentException e) {
      return notFound("unknown chart type " + chartType);
    }
    try {
      format = Format.valueOf(formatStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      return notFound("unknown chart format: " + formatStr);
    }
    final List<charts.Chart> charts = chartBuilder.getCharts(id,
        type, getRegions(request().queryString()), getParameters());
    for (charts.Chart chart : charts) {
      try {
        if(!modified(chart.created())) {
          return status(NOT_MODIFIED);
        }
      } catch(Exception e) {}
      try {
        final Representation r = chart.outputAs(format,
            getQueryDimensions(request().queryString()));
        ctx().response().setHeader("Last-Modified", asHttpDate(chart.created()));
        return ok(r.getContent()).as(r.getContentType());
      } catch (charts.Chart.UnsupportedFormatException e) {
        continue;
      }
    }
    return notFound();
  }

  private boolean fileIsAccessible(final String id) {
    return inUserSession(new F.Function<Session, Boolean>() {
      @Override
      public Boolean apply(Session session) throws Throwable {
        final FileStore.FileOrFolder fof = fileStore.getManager(session)
            .getByIdentifier(id);
        return fof != null && fof instanceof FileStore.File;
      }
    });
  }

  private String buildUrl(charts.Chart chart, String format, String id)
      throws UnsupportedEncodingException {
    return buildUrl(controllers.routes.Chart.chart(
            chart.getDescription().getType().toString().toLowerCase(),
            format, id), chart);
  }

  private String buildUrl(Call call, charts.Chart chart)
      throws UnsupportedEncodingException {
    return call.url() + (call.url().contains("?") ? "&" : "?") +
        URLEncodedUtils.format(getParameters(chart), "UTF-8");
  }

  private List<BasicNameValuePair> getParameters(charts.Chart chart) {
      List<BasicNameValuePair> l = Lists.newArrayList();
      l.add(new BasicNameValuePair("region",
          chart.getDescription().getRegion().getName()));
      for(String pname : chart.getDescription().getParameterNames()) {
          l.add(new BasicNameValuePair(pname,
              chart.getDescription().getParameter(pname).toString()));
      }
      return l;
  }

  public static List<FileStore.File> getFilesFromID(
      FileStore fileStore, Session session, String id) throws Exception {
    final FileStoreHelper fsh = new FileStoreHelper(session);
    final List<FileStore.File> files = Lists.newLinkedList();
    final FileStore.Manager fm = fileStore.getManager(session);
    FileStore.FileOrFolder fof = fm.getByIdentifier(id);
    if (fof instanceof FileStore.File) {
      files.add((FileStore.File) fof);
    } else if (fof instanceof FileStore.Folder) {
      files.addAll(fsh.listFilesInFolder((FileStore.Folder) fof));
    }
    return files;
  }

  protected static Set<String> getValues(Map<String, String[]> m, String key) {
    return ImmutableSet.copyOf(firstNonNull(m.get(key), new String[0]));
  }

  protected static List<Region> getRegions(Map<String, String[]> query) {
    return Lists.newArrayList(Region.getRegions(getValues(query, "region")));
  }

  protected static Dimension getQueryDimensions(Map<String, String[]> query) {
    final Dimension queryDimensions = new Dimension();
    try {
      double w = Double.parseDouble(getFirst(getValues(query, "width"), ""));
      queryDimensions.setSize(w, queryDimensions.getHeight());
    } catch (Exception e) {}
    try {
      double h = Double.parseDouble(getFirst(getValues(query, "height"), ""));
      queryDimensions.setSize(queryDimensions.getWidth(), h);
    } catch (Exception e) {}
    return queryDimensions;
  }

}
