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
import static org.apache.commons.lang.RandomStringUtils.randomAscii;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;
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

public class SearchTest {

  @Test
  public void searchContent() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fsm = fileStore().getManager(session);
        // Some Mark Twain quotes to search
        randomNestedFolder(fsm.getRoot(), 6).createFile(
            randomAlphanumeric(4),
            "text/plain", new ByteArrayInputStream((
                "Suppose you were an idiot and suppose you were a "+
                "member of Congress; but I repeat myself.").getBytes()));
        final String expectedId = randomNestedFolder(fsm.getRoot(), 6)
            .createFile(randomAlphanumeric(4),
                "text/plain", new ByteArrayInputStream((
                "Whenever you find yourself on the side of the majority, "+
                "it is time to pause and reflect.").getBytes()))
                .getIdentifier();
        final Result result = callAction(
            controllers.routes.ref.Search.search("majority"),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isEqualTo(1);
        for (JsonNode jsonObj : json) {
          assertThat(jsonObj.get("id")).isNotNull();
          assertThat(jsonObj.get("id").asText()).isEqualTo(expectedId);
          assertThat(jsonObj.get("score")).isNotNull();
          assertThat(jsonObj.get("score").asDouble()).isGreaterThan(0.0);
          assertThat(jsonObj.get("excerpt")).isNotNull();
          assertThat(jsonObj.get("type")).isNotNull();
          assertThat(jsonObj.get("type").asText()).isEqualTo("content");
        }
        return session;
      }
    });
  }

  @Test
  public void searchFilename() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fsm = fileStore().getManager(session);
        final String expectedId = randomNestedFolder(fsm.getRoot(), 6)
            .createFile(
                "The best laid schemes o' mice an' men",
                "appliction/octet-stream",
                new ByteArrayInputStream(randomAscii(1024).getBytes()))
            .getIdentifier();
        randomNestedFolder(fsm.getRoot(), 6)
            .createFile(
                "Gang aft agley",
                "appliction/octet-stream",
                new ByteArrayInputStream(randomAscii(1024).getBytes()));
        final Result result = callAction(
            controllers.routes.ref.Search.search("mice"),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final JsonNode json = Json.parse(contentAsString(result));
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isEqualTo(1);
        for (JsonNode jsonObj : json) {
          assertThat(jsonObj.get("id")).isNotNull();
          assertThat(jsonObj.get("id").asText()).isEqualTo(expectedId);
          assertThat(jsonObj.get("score")).isNotNull();
          assertThat(jsonObj.get("score").asDouble()).isGreaterThan(0.0);
          assertThat(jsonObj.get("excerpt")).isNotNull();
          assertThat(jsonObj.get("type")).isNotNull();
          assertThat(jsonObj.get("type").asText()).isEqualTo("filename");
        }
        return session;
      }
    });
  }

  @Test
  public void blankSearch() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.Search.search(""),
            newRequest);
        assertThat(status(result)).isEqualTo(400);
        return session;
      }
    });
  }


  public FileStore.Folder randomNestedFolder(FileStore.Folder root, int depth)
      throws RepositoryException {
    if (depth <= 0) {
      return root;
    }
    return randomNestedFolder(
        root.createFolder(randomAlphanumeric(8)),
        depth - 1);
  }

}
