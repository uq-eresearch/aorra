package controllers;

import static com.google.common.collect.Lists.newArrayList;
import static service.filestore.roles.Admin.isAdmin;

import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;

import play.Logger;
import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.JsonBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class GroupController extends SessionAwareController {

  @Inject
  public GroupController(JcrSessionFactory sessionFactory, Jcrom jcrom,
      CacheableUserProvider subjectHandler) {
    super(sessionFactory, jcrom, subjectHandler);
  }

  @SubjectPresent
  public Result list() {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        final GroupManager groupManager = new GroupManager(session);
        for (final Group group : groupManager.list()) {
          json.add(groupJson(session, group));
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result create() {
    final JsonNode json = request().body().asJson();
    if (!json.has("name")) {
      return badRequest("New groups require a name.");
    }
    return adminOnly(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final GroupManager groupManager = new GroupManager(session);
        final Group g = groupManager.create(json.get("name").asText());
        return created(groupJson(session, g))
            .as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result delete(final String id) {
    return adminOnly(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final GroupManager groupManager = new GroupManager(session);
        groupManager.delete(id);
        return noContent();
      }
    });
  }

  @SubjectPresent
  public Result get(final String id) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final GroupManager groupManager = new GroupManager(session);
        final Group g = groupManager.find(id);
        return ok(groupJson(session, g)).as("application/json; charset=utf-8");
      }
    });
  }

  protected JsonNode groupJson(Session session, final Group g)
      throws RepositoryException {
    final JsonBuilder jb = new JsonBuilder();
    final UserDAO dao = new UserDAO(session, jcrom);
    List<User> members = newArrayList();
    Iterator<Authorizable> iter = g.getMembers();
    while (iter.hasNext()) {
      final String jackrabbitUserId = iter.next().getID();
      final User user = dao.findByJackrabbitID(jackrabbitUserId);
      if (user != null) {
        members.add(user);
      }
    }
    return jb.toJson(g, members);
  }



  protected Result adminOnly(
      final F.Function<Session, Result> operation) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws Throwable {
        final UserDAO dao = new UserDAO(session, jcrom);
        if (!isAdmin(session, dao, dao.get(getUser()))) {
          return forbidden();
        }
        return operation.apply(session);
      }
    });
  }

}
