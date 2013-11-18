package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.injector;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Session;

import models.Flag;
import models.GroupManager;
import models.User;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.jackrabbit.api.security.user.Group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;

import play.api.mvc.AnyContentAsText;
import play.api.mvc.Call;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import service.filestore.CommentStore;
import service.filestore.FileStore;
import service.filestore.FileStore.Permission;
import service.filestore.FlagStore;
import service.filestore.FlagStore.FlagType;
import service.filestore.JsonBuilder;
import service.filestore.roles.Admin;

public class CommentControllerTest {

  @Test
  public void list() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final String targetId = UUID.randomUUID().toString();
        final CommentStore.Manager csm =
            injector().getInstance(CommentStore.class).getManager(session);
        csm.create(user.getId(), targetId, "Test message #1");
        csm.create(user.getId(), targetId, "Test message #2");
        final Result result = callAction(
            controllers.routes.ref.CommentController.list(targetId),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.isArray()).isTrue();
        for (JsonNode jsonObj : json) {
          System.out.println(jsonObj);
          assertThat(jsonObj.get("targetId").asText()).isEqualTo(targetId);
          assertThat(jsonObj.get("userId").asText()).isEqualTo(user.getId());
        }
        return session;
      }
    });
  }

}
