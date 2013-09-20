package controllers;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static play.api.test.Helpers.await;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.asAdminUserSession;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.loggedInRequest;
import static helpers.FileStoreHelper.XLS_MIME_TYPE;
import static helpers.FileStoreHelper.XLSX_MIME_TYPE;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import play.api.mvc.AsyncResult;
import play.api.mvc.Call;
import play.mvc.HandlerRef;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;
import service.filestore.FileStore;
import charts.representations.Format;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ChartTest {

  private static final String[] TTT_CHARTS = new String[] {
    "ttt_cane_and_hort","ttt_grazing","ttt_sediment","ttt_nitro_and_pest"
  };

  private static final String[] LAND_PS_CHARTS = new String[] {
    "horticulture_ps","sugarcane_ps","grains_ps"
  };

  @Test
  public void routes() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        {
          final Call call = controllers.routes.Chart.multipleFileCharts("svg",
              ImmutableList.<String>of());
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/charts?format=svg");
        }
        {
          final Call call = controllers.routes.Chart.multipleFileChart("marine",
              "svg",
              ImmutableList.<String>of());
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/charts/marine.svg");
        }
        {
          final String randomUUID = UUID.randomUUID().toString();
          final Call call = controllers.routes.Chart.singleFileCharts("svg",
              randomUUID);
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo(String.format(
              "/file/%s/charts?format=svg", randomUUID));
        }
        {
          final String randomUUID = UUID.randomUUID().toString();
          final Call call = controllers.routes.Chart.singleFileChart("marine",
              "svg", randomUUID);
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo(String.format(
              "/file/%s/charts/marine.svg", randomUUID));
        }
        return session;
      }
    });
  }

  @Test
  public void getCharts() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f = createMarineChartFile(session);
        final HandlerRef[] calls = new HandlerRef[] {
          controllers.routes.ref.Chart.multipleFileCharts("svg",
              ImmutableList.<String>of(f.getIdentifier())),
          controllers.routes.ref.Chart.singleFileCharts("svg",
              f.getIdentifier())
        };
        for (HandlerRef call : calls) {
          final Result result = callAction(call, newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.has("charts")).isTrue();
          assertThat(json.get("charts").isArray()).isTrue();
          assertThat(json.get("charts")).hasSize(7);
          for (JsonNode chartJson : (ArrayNode) json.get("charts")) {
            assertThat(chartJson.isObject()).as("chart is object").isTrue();
            assertThat(chartJson.has("type")).as("has type").isTrue();
            assertThat(chartJson.get("type").asText()).isEqualTo("Marine");
            assertThat(chartJson.has("region")).as("has region").isTrue();
            assertThat(chartJson.has("url")).as("has region").isTrue();
            assertThat(chartJson.get("url").asText()).contains("svg");
            assertThat(chartJson.get("url").asText())
              .contains(f.getIdentifier());
          }
        }
        return session;
      }
    });
  }

  @Test
  public void multiFileCharts() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f1 = createMarineChartFile(session);
        final FileStore.File f2 = createCOTOutbreakChartFile(session);
        final Result result = callAction(
            controllers.routes.ref.Chart.multipleFileCharts("svg",
                ImmutableList.<String>of(
                    f1.getIdentifier(), f2.getIdentifier())),
                newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.has("charts")).isTrue();
        assertThat(json.get("charts").isArray()).isTrue();
        assertThat(json.get("charts")).hasSize(8);
        for (JsonNode chartJson : (ArrayNode) json.get("charts")) {
          assertThat(chartJson.isObject()).as("chart is object").isTrue();
          assertThat(chartJson.has("type")).as("has type").isTrue();
          assertThat(chartJson.has("region")).as("has region").isTrue();
          assertThat(chartJson.has("url")).as("has region").isTrue();
          assertThat(chartJson.get("url").asText()).contains("svg");
        }
        return session;
      }
    });
  }

  @Test
  public void getNoCharts() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Folder folder =
            fileStore().getManager(session).getRoot();
        final FileStore.File f = folder.createFile("test.txt",
            "text/plain",
            new ByteArrayInputStream("Test content.".getBytes()));
        // Try with file that isn't a spreadsheet
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileCharts("svg",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.has("charts")).isTrue();
          assertThat(json.get("charts").isArray()).isTrue();
          assertThat(json.get("charts")).hasSize(0);
        }
        // Try with folder
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileCharts("svg",
                  ImmutableList.<String>of(folder.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.has("charts")).isTrue();
          assertThat(json.get("charts").isArray()).isTrue();
          assertThat(json.get("charts")).hasSize(0);
        }
        return session;
      }
    });
  }

  @Test
  public void svgChart() {
    asAdminUser(
        new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        checkChart("marine",
            createMarineChartFile(session), newRequest);
        checkChart("marine",
            createMarineXlsChartFile(session), newRequest);
        checkChart("annual_rainfall",
            createAnnualRainfallChartFile(session), newRequest);
        checkChart("cots_outbreak",
            createCOTOutbreakChartFile(session), newRequest);
        checkChart("progress_table",
            createProgressTableChartFile(session), newRequest);
        checkChart("grazing_ps",
            createGrazingPracticeChartFile(session), newRequest);
        for (String c : LAND_PS_CHARTS) {
          checkChart(c, createLandPracticeChartFile(session, c), newRequest);
        }
        for (String c : TTT_CHARTS) {
          checkChart(c, createTrackingTowardsTargetsChartFile(session, c),
              newRequest);
        }
        return session;
      }
      private void checkChart(
          final String chartType,
          final FileStore.File f,
          final FakeRequest newRequest) {
        final HandlerRef[] calls = new HandlerRef[] {
            controllers.routes.ref.Chart.multipleFileChart(chartType, "svg",
                ImmutableList.<String>of(f.getIdentifier())),
            controllers.routes.ref.Chart.singleFileChart(chartType, "svg",
                f.getIdentifier())
          };
        for (HandlerRef call : calls) {
          final Result result = callAction(call, newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("image/svg+xml");
        }
      }
    });
    asAdminUserSession(
        new F.Function3<Session, User, Http.Session, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final Http.Session httpSession) throws Throwable {
        checkChartWithRegions("marine", Arrays.asList("Cape York"),
            createMarineChartFile(session), httpSession);
        checkChartWithRegions("annual_rainfall", Arrays.asList("Fitzroy"),
            createAnnualRainfallChartFile(session), httpSession);
        return session;
      }
      private void checkChartWithRegions(
          final String chartType,
          final List<String> regions,
          final FileStore.File f,
          final Http.Session httpSession) {
        final List<String> pairs = Lists.newLinkedList();
        for (String r : regions) {
          pairs.add("region="+r);
        }
        final String qs = Joiner.on("&").join(pairs);
        final Result result = callAction(
            controllers.routes.ref.Chart.multipleFileChart(chartType, "svg",
                ImmutableList.<String>of(f.getIdentifier())),
            loggedInRequest(new FakeRequest("GET", "?"+qs), httpSession));
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/svg+xml");
      }
    });
  }

  @Test
  public void svgChartSize() {
    asAdminUserSession(
        new F.Function3<Session, User, Http.Session, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final Http.Session httpSession) throws Throwable {
        final FileStore.File f = createMarineChartFile(session);
        {
          final List<String> pairs = asList();
          Element svg = getSvgRoot(httpSession, f.getIdentifier(), pairs);
          String[] viewBox = svg.attr("viewBox").split(" ");
          assertThat(svg.attr("width")).isEqualTo(viewBox[2]);
          assertThat(svg.attr("height")).isEqualTo(viewBox[3]);
        }
        {
          final List<String> pairs = asList("width=1337");
          Element svg = getSvgRoot(httpSession, f.getIdentifier(), pairs);
          assertThat(svg.attr("width")).isEqualTo("1337");
          assertThat(svg.attr("height")).isNotEqualTo("0");
        }
        {

          final List<String> pairs = asList("height=1337");
          Element svg = getSvgRoot(httpSession, f.getIdentifier(), pairs);
          assertThat(svg.attr("width")).isNotEqualTo("0");
          assertThat(svg.attr("height")).isEqualTo("1337");
        }
        {

          final List<String> pairs = asList("width=9832", "height=1337");
          Element svg = getSvgRoot(httpSession, f.getIdentifier(), pairs);
          assertThat(svg.attr("width")).isEqualTo("9832");
          assertThat(svg.attr("height")).isEqualTo("1337");
        }
        return session;
      }
      private Element getSvgRoot(
          final Http.Session httpSession,
          final String fileId,
          final List<String> pairs) {
        final String qs = Joiner.on("&").join(pairs);
        final Result result = callAction(
            controllers.routes.ref.Chart.singleFileChart("marine", "svg",
                fileId),
            loggedInRequest(new FakeRequest("GET", "?"+qs), httpSession));
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/svg+xml");
        final Document doc = Jsoup.parse(contentAsString(result));
        return doc.select("svg").get(0);
      }
    });
  }

  @Test
  public void unknownChartTypeOrFormat() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f = createMarineChartFile(session);
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("unknown", "svg",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(404);
        }
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("marine",
                  "unknown", ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(404);
        }
        return session;
      }
    });
  }

  @Test
  public void otherChartFormats() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      private final Format[] OTHER_FORMATS = new Format[] {
        Format.PNG,
        Format.EMF,
        Format.DOCX
      };

      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        checkChart("marine",
            createMarineChartFile(session), newRequest);
        checkChart("marine",
            createMarineXlsChartFile(session), newRequest);
        return session;
      }
      private void checkChart(
          final String chartType,
          final FileStore.File f,
          final FakeRequest newRequest) {
        for (Format format : OTHER_FORMATS) {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart(chartType,
                  format.name(),
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(waitForReady(result, 120))).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo(format.getMimeType());
        }
      }
    });
  }

  @Test
  public void csvChart() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        // Marine chart CSV
        {
          final FileStore.File f = createMarineChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("marine", "csv",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/csv");
          assertThat(charset(result)).isEqualTo("utf-8");
          final ICsvListReader listReader = new CsvListReader(
              new StringReader(contentAsString(result)),
              CsvPreference.STANDARD_PREFERENCE);
          int rowCount = 0;
          List<String> row;
          while ((row = listReader.read()) != null) {
            assertThat(row).hasSize(2);
            // GBR values are all MODERATE
            assertThat(row.get(1)).isEqualTo("Moderate");
            rowCount++;
          }
          assertThat(rowCount).isEqualTo(13);
          listReader.close();
        }
        // COT chart CSV
        {
          final FileStore.File f = createCOTOutbreakChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("cots_outbreak",
                  "csv",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/csv");
          assertThat(charset(result)).isEqualTo("utf-8");
          final ICsvListReader listReader = new CsvListReader(
              new StringReader(contentAsString(result)),
              CsvPreference.STANDARD_PREFERENCE);
          int rowCount = 0;
          List<String> row = listReader.read();
          assertThat(row).contains("Year", "Outbreaks");
          while ((row = listReader.read()) != null) {
            assertThat(row).hasSize(2);
            // COT years start at 1998
            assertThat(row.get(0)).isEqualTo((1998+rowCount)+"");
            // COT outbreaks start at 29, decreasing by two each year
            assertThat(row.get(1)).isEqualTo((29-(2*rowCount))+"");
            rowCount++;
          }
          assertThat(rowCount).isEqualTo(12);
          listReader.close();
        }
        // Rainfall
        {
          final FileStore.File f = createAnnualRainfallChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("annual_rainfall",
                  "csv",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/csv");
          assertThat(charset(result)).isEqualTo("utf-8");
          final ICsvListReader listReader = new CsvListReader(
              new StringReader(contentAsString(result)),
              CsvPreference.STANDARD_PREFERENCE);
          int rowCount = 0;
          List<String> row = listReader.read();
          assertThat(row).contains("Year", "Railfall (mm)");
          while ((row = listReader.read()) != null) {
            assertThat(row).hasSize(2);
            // Rainfall years start at 1988
            assertThat(row.get(0)).isEqualTo((1988+rowCount)+"");
            // Test rainfall data starts at 105, increasing by one each year
            assertThat(row.get(1)).isEqualTo((105+rowCount)+".0");
            rowCount++;
          }
          assertThat(rowCount).isEqualTo(24);
          listReader.close();
        }
        // Progress
        {
          final String[] condStr = new String[] {
            null, "Very Good", "Good", "Moderate", "Poor", "Very Poor"
          };
          final int[][] expected = new int[][] {
            new int[] { 3, 2, 3, 1, 3, 1, 2 },
            new int[] { 1, 0, 2, 0, 4, 2, 0 },
            new int[] { 3, 2, 3, 1, 4, 2, 3 },
            new int[] { 2, 3, 3, 1, 4, 1, 2 },
            new int[] { 1, 2, 1, 1, 2, 5, 1 },
            new int[] { 3, 3, 4, 1, 4, 2, 3 },
            new int[] { 3, 1, 3, 1, 3, 2, 5 }
          };
          final FileStore.File f = createProgressTableChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("progress_table",
                  "csv",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/csv");
          assertThat(charset(result)).isEqualTo("utf-8");
          final ICsvListReader listReader = new CsvListReader(
              new StringReader(contentAsString(result)),
              CsvPreference.STANDARD_PREFERENCE);
          int rowCount = 0;
          List<String> row = listReader.read();
          {
            final String[] headers = new String[] {
                "", "Grazing", "Sugarcane", "Horticulture",
                "Groundcover", "Nitrogen", "Sediment", "Pesticides" };
            for (int i = 1; i < row.size(); i++) {
              assertThat(row.get(i)).isEqualTo(headers[i]);
            }
          }
          while ((row = listReader.read()) != null) {
            for (int i = 1; i < row.size(); i++) {
              if (row.get(i) != null) {
                assertThat(row.get(i)).contains("%");
              }
              final String condition = condStr[expected[rowCount][i-1]];
              if (condition == null) {
                assertThat(row.get(i)).isNull();
              } else {
                assertThat(row.get(i)).startsWith(condition);
              }
            }
            rowCount++;
          }
          assertThat(rowCount).isEqualTo(7);
          listReader.close();
        }
        return session;
      }
    });
  }

  @Test
  public void commentary() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        // Marine chart CSV
        {
          final FileStore.File f = createMarineChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("marine", "html",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(contentAsString(result)).isEqualTo(
            "<p>This is the <em>Great Barrier Reef</em> overview.</p>\n"+
            "<ul>\n"+
            "  <li>Write in Markdown</li>\n"+
            "  <li>It's converted to HTML</li>\n"+
            "</ul>");
        }
        {
          final FileStore.File f = createTrackingTowardsTargetsChartFile(
              session, "grazing");
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("ttt_grazing",
                  "html",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(contentAsString(result)).isEqualTo(
            "<p><em>Nothing yet.</em></p>");
        }
        return session;
      }
    });
  }

  @Test
  public void noChart() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f = createMarineChartFile(session);
        // Invalid type
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("foobar", "svg",
                  ImmutableList.<String>of(f.getIdentifier())),
              newRequest);
          assertThat(status(result)).isEqualTo(404);
        }
        // Missing path
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.multipleFileChart("marine", "svg",
                  ImmutableList.<String>of(UUID.randomUUID().toString())),
              newRequest);
          assertThat(status(result)).isEqualTo(404);
        }
        return session;
      }
    });
  }

  public FileStore.File createMarineChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("marine.xlsx",
        XLSX_MIME_TYPE,
        new FileInputStream("test/marine.xlsx"));
  }

  // Old-style office document
  private FileStore.File createMarineXlsChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder  = fileStore().getManager(session).getRoot();
    return folder.createFile("marine.xls",
        XLS_MIME_TYPE,
        new FileInputStream("test/marine.xls"));
  }

  private FileStore.File createCOTOutbreakChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("cots_outbreak.xlsx",
        XLSX_MIME_TYPE,
        new FileInputStream("test/cots_outbreak.xlsx"));
  }

  private FileStore.File createAnnualRainfallChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("annual_rainfall.xlsx",
        XLSX_MIME_TYPE,
        new FileInputStream("test/annual_rainfall.xlsx"));
  }

  private FileStore.File createProgressTableChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("progress_table.xlsx",
        XLSX_MIME_TYPE,
        new FileInputStream("test/progress_table.xlsx"));
  }

  private FileStore.File createGrazingPracticeChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("grazing_practice_systems.xlsx",
        XLSX_MIME_TYPE,
        new FileInputStream("test/grazing_practice_systems.xlsx"));
  }

  private FileStore.File createLandPracticeChartFile(final Session session,
      final String prefix)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile(prefix + ".xlsx", XLSX_MIME_TYPE,
        new FileInputStream("test/land_practice_systems.xlsx"));
  }

  private FileStore.File createTrackingTowardsTargetsChartFile(
      final Session session, final String prefix) throws RepositoryException,
      FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile(prefix + ".xlsx", XLSX_MIME_TYPE,
        new FileInputStream("test/tracking_towards_targets.xlsx"));
  }

  private Result waitForReady(Result result, int seconds) {
    await(((AsyncResult)result.getWrappedResult()).result(),
        seconds, TimeUnit.SECONDS);
    return result;
  }

}
