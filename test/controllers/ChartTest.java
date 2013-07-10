package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;

import javax.jcr.Session;

import models.User;

import org.junit.Test;

import play.api.mvc.Call;
import play.libs.F;
import play.mvc.Result;
import play.test.FakeRequest;

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
          final Call call =
              controllers.routes.Chart.charts("");
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/charts/");
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
        final Result result = callAction(
            controllers.routes.ref.Chart.charts(""),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        return session;
      }
    });
  }


}
