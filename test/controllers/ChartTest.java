package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.asAdminUserSession;
import static test.AorraTestUtils.loggedInRequest;
import static test.AorraTestUtils.fileStore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import play.api.mvc.Call;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;
import service.filestore.FileStore;

public class ChartTest {

  @Test
  public void routes() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        {
          final Call call = controllers.routes.Chart.charts("svg",
              ImmutableList.<String>of());
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/charts/?format=svg");
        }
        {
          final Call call = controllers.routes.Chart.chart("marine", "svg",
              ImmutableList.<String>of());
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/charts/marine.svg");
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
        final Result result = callAction(
            controllers.routes.ref.Chart.charts("svg",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
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
          assertThat(chartJson.get("type").asText()).isEqualTo("MARINE");
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
              controllers.routes.ref.Chart.charts("svg",
                  ImmutableList.<String>of(f.getPath())),
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
              controllers.routes.ref.Chart.charts("svg",
                  ImmutableList.<String>of(folder.getPath())),
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
        checkChart("annual_rainfall",
            createAnnualRainfallChartFile(session), newRequest);
        checkChart("cots_outbreak",
            createCOTOutbreakChartFile(session), newRequest);
        checkChart("progress_table",
            createProgressTableChartFile(session), newRequest);
        return session;
      }
      private void checkChart(
          final String chartType,
          final FileStore.File f,
          final FakeRequest newRequest) {
        final Result result = callAction(
            controllers.routes.ref.Chart.chart(chartType, "svg",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/svg+xml");
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
            controllers.routes.ref.Chart.chart(chartType, "svg",
                ImmutableList.<String>of(f.getPath())),
            loggedInRequest(new FakeRequest("GET", "?"+qs), httpSession));
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/svg+xml");
      }
    });
  }

  @Test
  public void pngChart() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        checkChart("marine",
            createMarineChartFile(session), newRequest);
        checkChart("annual_rainfall",
            createAnnualRainfallChartFile(session), newRequest);
        checkChart("cots_outbreak",
            createCOTOutbreakChartFile(session), newRequest);
        checkChart("progress_table",
            createProgressTableChartFile(session), newRequest);
        return session;
      }
      private void checkChart(
          final String chartType,
          final FileStore.File f,
          final FakeRequest newRequest) {
        final Result result = callAction(
            controllers.routes.ref.Chart.chart(chartType, "png",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/png");
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
              controllers.routes.ref.Chart.chart("marine", "csv",
                  ImmutableList.<String>of(f.getPath())),
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
              controllers.routes.ref.Chart.chart("cots_outbreak", "csv",
                  ImmutableList.<String>of(f.getPath())),
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
              controllers.routes.ref.Chart.chart("annual_rainfall", "csv",
                  ImmutableList.<String>of(f.getPath())),
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
          final FileStore.File f = createProgressTableChartFile(session);
          final Result result = callAction(
              controllers.routes.ref.Chart.chart("progress_table", "csv",
                  ImmutableList.<String>of(f.getPath())),
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
              controllers.routes.ref.Chart.chart("foobar", "svg",
                  ImmutableList.<String>of(f.getPath())),
              newRequest);
          assertThat(status(result)).isEqualTo(404);
        }
        // Missing path
        {
          final Result result = callAction(
              controllers.routes.ref.Chart.chart("marine", "svg",
                  ImmutableList.<String>of("/doesnotexist")),
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
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/marine.xlsx"));
  }

  public FileStore.File createCOTOutbreakChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("cots_outbreak.xlsx",
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/cots_outbreak.xlsx"));
  }

  public FileStore.File createAnnualRainfallChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("annual_rainfall.xlsx",
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/annual_rainfall.xlsx"));
  }

  public FileStore.File createProgressTableChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("progress_table.xlsx",
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/progress_table.xlsx"));
  }

}
