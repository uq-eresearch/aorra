package controllers;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jcrom.Jcrom;
import org.w3c.dom.Document;

import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;
import be.objectify.deadbolt.java.actions.SubjectPresent;
import charts.ChartDescription;
import charts.ChartFactory;
import charts.ChartRenderer;
import charts.ChartType;
import charts.DataSource;
import charts.XlsxDataSource;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Chart extends SessionAwareController {

  public static final String XLSX_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final FileStore fileStore;

  @Inject
  public Chart(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
      final CacheableUserProvider sessionHandler, final FileStore fileStore) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStore = fileStore;
  }

  private String buildUrl(charts.Chart chart, String format,
      List<String> paths) throws UnsupportedEncodingException {
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    for (Map.Entry<String, String> me : chart.getDescription().getProperties()
        .entrySet()) {
      qparams.add(new BasicNameValuePair(me.getKey(), me.getValue()));
    }
    return controllers.routes.Chart.chart(
        chart.getDescription().getType().toString().toLowerCase(),
        format, paths).url() + "&" + URLEncodedUtils.format(qparams, "UTF-8");
  }

  private List<DataSource> getDatasources(Session session, List<String> paths)
      throws Exception {
    List<DataSource> result = Lists.newArrayList();
    final FileStore.Manager fm = fileStore.getManager(session);
    for (String path : paths) {
      FileStore.FileOrFolder fof = fm.getFileOrFolder(path);
      if (fof instanceof FileStoreImpl.File) {
        FileStoreImpl.File file = (FileStoreImpl.File) fof;
        // Check this is an OpenXML document (no chance otherwise)
        if (file.getMimeType().equals(XLSX_MIME_TYPE)) {
          result.add(new XlsxDataSource(file.getData()));
        }
      }
    }
    return result;
  }

  @SubjectPresent
  public Result charts(final String format, final List<String> paths) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Exception {
        List<DataSource> datasources = getDatasources(session, paths);
        ChartFactory f = new ChartFactory(datasources);
        List<charts.Chart> charts = f.getCharts(request().queryString());
        final ObjectNode json = Json.newObject();
        final ArrayNode aNode = json.putArray("charts");
        for (charts.Chart chart : charts) {
          ChartDescription desc = chart.getDescription();
          final ObjectNode chartNode = Json.newObject();
          chartNode.put("type", desc.getType().toString());
          for (Map.Entry<String, String> me : desc.getProperties().entrySet()) {
            chartNode.put(me.getKey(), me.getValue());
          }
          chartNode.put("url",
              buildUrl(chart, format, paths));
          aNode.add(chartNode);
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result chart(final String chart, final String format, final List<String> paths) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Exception {
        List<DataSource> datasources = getDatasources(session, paths);
        ChartFactory f = new ChartFactory(datasources);
        final ChartType type;
        try {
          type = ChartType.getChartType(chart);
        } catch (IllegalArgumentException e) {
          return notFound("unknown chart type " + chart);
        }
        List<charts.Chart> charts = f.getCharts(type, request()
            .queryString());
        if (charts.isEmpty()) {
          return notFound();
        } else {
          ChartRenderer renderer = new ChartRenderer(charts.get(0).getChart());
          String svg = renderer.render();
          return toFormat(svg, format);
        }
      }
    });
  }

  private Result toFormat(String svg, String format) throws Exception {
    if ("png".equals(format)) {
      Document doc = toDocument(svg);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      new PNGTranscoder().transcode(new TranscoderInput(doc),
          new TranscoderOutput(os));
      return ok(os.toByteArray()).as("image/png");
    } else {
      return ok(svg).as("image/svg+xml");
    }
  }

  private Document toDocument(String svg) throws Exception {
    // Turn back into DOM
    String parserName = XMLResourceDescriptor.getXMLParserClassName();
    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parserName);
    Document doc = f.createDocument("file:///test.svg",
        new CharArrayReader(svg.toCharArray()));
    String h = doc.getDocumentElement().getAttribute("height");
    String w = doc.getDocumentElement().getAttribute("width");
    doc.getDocumentElement().setAttributeNS(null, "viewbox",
        String.format("0 0 %s %s", w, h));
    // if (relativeDimensions) {
    // doc.getDocumentElement().setAttribute("height", "100%")
    // doc.getDocumentElement().setAttribute("width", "100%")
    // }
    return doc;
  }
}
