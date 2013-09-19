package controllers;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.getFirst;
import static java.util.Arrays.asList;
import helpers.FileStoreHelper;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
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
import charts.Chart.UnsupportedFormatException;
import charts.ChartDescription;
import charts.ChartType;
import charts.Region;
import charts.builder.ChartBuilder;
import charts.builder.DataSource;
import charts.representations.Format;
import charts.representations.Representation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
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
  public Result chartArchive(final String id) throws IOException {
    final File tempFile = File.createTempFile("zipfile", "");
    final ZipOutputStream zos =
        new ZipOutputStream(new FileOutputStream(tempFile));
    zos.setMethod(ZipOutputStream.DEFLATED);
    zos.setLevel(5);
    inUserSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) throws Throwable {
        final Map<FileStore.File, DataSource> datasources =
            getDatasourcesFromIDs(session, asList(id));
        for (FileStore.File file : datasources.keySet()) {
          final DataSource datasource = datasources.get(file);
          final List<charts.Chart> charts = chartBuilder.getCharts(
              asList(datasource), asList(Region.values()), new Dimension());
          for (charts.Chart chart : charts) {
            for (Format f : Format.values()) {
              final String filepath = String.format("%s/%s-%s-%s.%s\n",
                  id,
                  chart.getDescription().getType(),
                  chart.getDescription().getRegion(),
                  file.getIdentifier(),
                  f.name()).trim().toLowerCase();
              final byte[] data;
              try {
                data = chart.outputAs(f).getContent();
              } catch (UnsupportedFormatException e) {
                continue; // Skip to next format
              }
              zos.putNextEntry(new ZipEntry(filepath));
              IOUtils.write(data, zos);
              zos.closeEntry();
            }
          }
        }
        return session;
      }
    });
    zos.close();
    ctx().response().setContentType("application/zip");
    ctx().response().setHeader("Content-Disposition",
        "attachment; filename="+id+".zip");
    ctx().response().setHeader("Content-Length", tempFile.length()+"");
    return ok(new FileInputStream(tempFile) {
      @Override
      public void close() throws IOException {
        super.close();
        tempFile.delete();
      }
    });
  }

  @SubjectPresent
  public Result multipleFileCharts(final String format, final List<String> ids) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Exception {
        final List<DataSource> datasources = Lists.newArrayList(
            getDatasourcesFromIDs(session, ids).values());
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
        final List<DataSource> datasources = Lists.newArrayList(
            getDatasourcesFromIDs(session, ids).values());
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

  private Map<FileStore.File, DataSource> getDatasourcesFromIDs(Session session,
      List<String> ids) throws Exception {
    final FileStoreHelper fsh = new FileStoreHelper(session);
    final List<FileStore.File> files = Lists.newLinkedList();
    final FileStore.Manager fm = fileStore.getManager(session);
    for (String id : ids) {
      FileStore.FileOrFolder fof = fm.getByIdentifier(id);
      if (fof instanceof FileStore.File) {
        files.add((FileStore.File) fof);
      } else if (fof instanceof FileStore.Folder) {
        files.addAll(fsh.listFilesInFolder((FileStore.Folder) fof));
      }
    }
    return fsh.getDatasources(files);
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
