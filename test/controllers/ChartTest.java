package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Ignore;
import org.junit.Test;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import com.google.common.collect.ImmutableList;

import play.api.mvc.Call;
import play.libs.F;
import play.libs.Json;
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
        assertThat(header("Cache-Control", result)).isEqualTo("max-age=0, must-revalidate");
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
        final FileStore.Folder folder = fileStore().getManager(session).getRoot();
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
          assertThat(header("Cache-Control", result)).isEqualTo("max-age=0, must-revalidate");
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
          assertThat(header("Cache-Control", result)).isEqualTo("max-age=0, must-revalidate");
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
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f = createMarineChartFile(session);
        final Result result = callAction(
            controllers.routes.ref.Chart.chart("marine", "svg",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/svg+xml");
        return session;
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
        final FileStore.File f = createMarineChartFile(session);
        final Result result = callAction(
            controllers.routes.ref.Chart.chart("marine", "png",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("image/png");
        return session;
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
          final FileStore.File f = createCOTChartFile(session);
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

  public FileStore.File createCOTChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("cot.xlsx",
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/cots.xlsx"));
  }

  public FileStore.File createAnnualRainfallChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("annual_rainfall.xlsx",
        Chart.XLSX_MIME_TYPE,
        new FileInputStream("test/annual_rainfall.xlsx"));
  }


}
