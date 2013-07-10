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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;

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
        final FileStore.File f = createChartFile(session);
        final Result result = callAction(
            controllers.routes.ref.Chart.charts("svg",
                ImmutableList.<String>of(f.getPath())),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
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
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
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
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
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
        final FileStore.File f = createChartFile(session);
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
        final FileStore.File f = createChartFile(session);
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
  public void noChart() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.File f = createChartFile(session);
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

  public FileStore.File createChartFile(final Session session)
      throws RepositoryException, FileNotFoundException {
    final FileStore.Folder folder = fileStore().getManager(session).getRoot();
    return folder.createFile("chart.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        new FileInputStream("test/chart_01.xlsx"));
  }


}
