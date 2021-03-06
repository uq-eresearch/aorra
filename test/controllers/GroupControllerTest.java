package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static notifications.NotificationManagerTest.awaitNotifications;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.flagStore;
import static test.AorraTestUtils.jcrom;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import models.Flag;
import models.GroupManager;
import models.Notification;
import models.User;
import models.UserDAO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Test;

import play.api.mvc.Call;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;

public class GroupControllerTest {

  @Test
  public void routes() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        {
          final Call call = controllers.routes.GroupController.list();
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/groups");
        }
        return session;
      }
    });
  }

  @Test
  public void list() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.GroupController.list(),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.isArray()).isTrue();
        assertThat(json).hasSize(1);
        for (JsonNode g : json) {
          assertThat(g.has("id")).isTrue();
          assertThat(g.has("name")).isTrue();
          assertThat(g.has("members")).isTrue();
        }
        return session;
      }
    });
  }

  @Test
  public void get() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.GroupController.get("testgroup"),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.isArray()).isFalse();
        assertThat(json.has("id")).isTrue();
        assertThat(json.has("name")).isTrue();
        assertThat(json.has("members")).isTrue();
        return session;
      }
    });
  }

  @Test
  public void create() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        // name attribute is required
        {
          final ObjectNode jsonRequest = Json.newObject();
          final Result result = callAction(
              controllers.routes.ref.GroupController.create(),
              newRequest.withJsonBody(jsonRequest));
          assertThat(status(result)).isEqualTo(400);
        }
        // New group can be created
        {
          final ObjectNode jsonRequest = Json.newObject();
          jsonRequest.put("name", "newgroup");
          final Result result = callAction(
              controllers.routes.ref.GroupController.create(),
              newRequest.withJsonBody(jsonRequest));
          assertThat(new GroupManager(session).find("newgroup")).isNotNull();
          assertThat(status(result)).isEqualTo(201);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.isArray()).isFalse();
          assertThat(json.has("id")).isTrue();
          assertThat(json.has("name")).isTrue();
          assertThat(json.has("members")).isTrue();
        }
        return session;
      }
    });
  }

  @Test
  public void update() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final GroupManager gm = new GroupManager(session);
        final Group group = gm.create("newgroup");
        // members attribute is required
        {
          final ObjectNode jsonRequest = Json.newObject();
          final Result result = callAction(
              controllers.routes.ref.GroupController.update("newgroup"),
              newRequest.withJsonBody(jsonRequest));
          assertThat(status(result)).isEqualTo(400);
        }
        // Members can be added and removed
        {
          final ObjectNode jsonRequest = Json.newObject();
          jsonRequest.putArray("members").add(user.getId());
          final Result result = callAction(
              controllers.routes.ref.GroupController.update("newgroup"),
              newRequest.withJsonBody(jsonRequest));
          assertThat(gm.memberships(user.getId()))
            .contains(group);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.isArray()).isFalse();
          assertThat(json.has("id")).isTrue();
          assertThat(json.has("name")).isTrue();
          assertThat(json.has("members")).isTrue();
        }
        {
          final ObjectNode jsonRequest = Json.newObject();
          jsonRequest.putArray("members");
          final Result result = callAction(
              controllers.routes.ref.GroupController.update("newgroup"),
              newRequest.withJsonBody(jsonRequest));
          assertThat(gm.memberships(user.getId()))
            .excludes(group);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result))
            .isEqualTo("max-age=0, must-revalidate");
          final JsonNode json = Json.parse(contentAsString(result));
          assertThat(json.isArray()).isFalse();
          assertThat(json.has("id")).isTrue();
          assertThat(json.has("name")).isTrue();
          assertThat(json.has("members")).isTrue();
        }
        // Removing admin == removed access
        {
          final ObjectNode jsonRequest = Json.newObject();
          jsonRequest.putArray("members");
          final Result result = callAction(
              controllers.routes.ref.GroupController.update("testgroup"),
              newRequest.withJsonBody(jsonRequest));
          assertThat(status(result)).isEqualTo(200);
        }
        {
          final ObjectNode jsonRequest = Json.newObject();
          jsonRequest.putArray("members").add(user.getId());
          final Result result = callAction(
              controllers.routes.ref.GroupController.update("testgroup"),
              newRequest.withJsonBody(jsonRequest));
          assertThat(status(result)).isEqualTo(403);
        }
        return session;
      }
    });
  }

  @Test
  public void delete() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.GroupController.delete("testgroup"),
            newRequest);
        try {
          new GroupManager(session).find("testgroup");
          fail();
        } catch (PathNotFoundException e) {
          // As expected
        }
        assertThat(status(result)).isEqualTo(204);
        return session;
      }
    });
  }

}
