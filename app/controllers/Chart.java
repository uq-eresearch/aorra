package controllers;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.getFirst;

import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jcrom.Jcrom;

import play.libs.F;
import play.libs.Json;
import play.mvc.Call;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;
import charts.ChartDescription;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartBuilder;
import charts.builder.DataSource;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;
import charts.representations.Representation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Chart extends SessionAwareController {

  public static final String XLSX_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  public static final String XLS_MIME_TYPE =
      "application/vnd.ms-excel";

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
  public Result multipleFileCharts(final String format, final List<String> ids) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Exception {
        List<DataSource> datasources = getDatasourcesFromIDs(session, ids);
        final List<charts.Chart> charts =
            chartBuilder.getCharts(datasources,
                getRegions(request().queryString()),
                getQueryDimensions(request().queryString()));
        final ObjectNode json = Json.newObject();
        final ArrayNode aNode = json.putArray("charts");
        for (charts.Chart chart : charts) {
          ChartDescription desc = chart.getDescription();
          final ObjectNode chartNode = Json.newObject();
          chartNode.put("type", desc.getType().getLabel());
          chartNode.put("region", desc.getRegion().getName());
          chartNode.put("url",
              ids.size() == 1
              ? buildUrl(chart, format, ids.get(0))
              : buildUrl(chart, format, ids));
          aNode.add(chartNode);
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result singleFileCharts(final String format, final String id) {
    return multipleFileCharts(format, ImmutableList.of(id));
  }

  @SubjectPresent
  public Result multipleFileChart(final String chartType,
      final String formatStr, final List<String> ids) {
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
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Exception {
        final List<DataSource> datasources =
            getDatasourcesFromIDs(session, ids);
        final List<charts.Chart> charts = chartBuilder.getCharts(datasources,
            type, getRegions(request().queryString()),
            getQueryDimensions(request().queryString()));
        for (charts.Chart chart : charts) {
          try {
            final Representation r = chart.outputAs(format);
            return ok(r.getContent()).as(r.getContentType());
          } catch (charts.Chart.UnsupportedFormatException e) {
            continue;
          }
        }
        return notFound();
      }
    });
  }

  @SubjectPresent
  public Result singleFileChart(final String chartType, final String formatStr,
      final String id) {
    return multipleFileChart(chartType, formatStr, ImmutableList.of(id));
  }

  private String buildUrl(charts.Chart chart, String format,
      List<String> ids) throws UnsupportedEncodingException {
    return buildUrl(controllers.routes.Chart.multipleFileChart(
            chart.getDescription().getType().toString().toLowerCase(),
            format, ids), chart);
  }

  private String buildUrl(charts.Chart chart, String format, String id)
      throws UnsupportedEncodingException {
    return buildUrl(controllers.routes.Chart.singleFileChart(
            chart.getDescription().getType().toString().toLowerCase(),
            format, id), chart);
  }

  private String buildUrl(Call call, charts.Chart chart)
      throws UnsupportedEncodingException {
    return call.url() + (call.url().contains("?") ? "&" : "?") +
        URLEncodedUtils.format(
            ImmutableList.of(new BasicNameValuePair("region",
                chart.getDescription().getRegion().getName())), "UTF-8");
  }

  private List<DataSource> getDatasourcesFromIDs(Session session,
      List<String> ids) throws Exception {
    List<FileStore.File> files = Lists.newLinkedList();
    final FileStore.Manager fm = fileStore.getManager(session);
    for (String id : ids) {
      FileStore.FileOrFolder fof = fm.getByIdentifier(id);
      if (fof instanceof FileStoreImpl.File) {
        files.add((FileStore.File) fof);
      }
    }
    return getDatasources(session, files);
  }

  private List<DataSource> getDatasources(Session session,
      Iterable<FileStore.File> files) throws Exception {
    List<DataSource> result = Lists.newLinkedList();
    for (FileStore.File file : files) {
      if(file.getMimeType().equals(XLS_MIME_TYPE)) {
        result.add(new XlsDataSource(file.getData()));
      } else if (file.getMimeType().equals(XLSX_MIME_TYPE)) {
        // Check this is an OpenXML document (no chance otherwise)
        result.add(new XlsxDataSource(file.getData()));
      }
    }
    return result;
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
