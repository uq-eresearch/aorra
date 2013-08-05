package controllers;

import static service.filestore.roles.Admin.isAdmin;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Notification;
import models.User;
import models.UserDAO;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.JsonBuilder;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.google.inject.Inject;

@With(UncacheableAction.class)
public final class UserController extends SessionAwareController {

  @Inject
  UserController(JcrSessionFactory sessionFactory, Jcrom jcrom,
      CacheableUserProvider subjectHandler) {
    super(sessionFactory, jcrom, subjectHandler);
  }

  @SubjectPresent
  public Result usersJson() {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        final UserDAO dao = new UserDAO(session, jcrom);
        for (final User user : dao.list()) {
          json.add(jb.toJson(user, isAdmin(session, dao, user)));
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result notificationsJson() {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final UserDAO dao = new UserDAO(session, jcrom);
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        for (final Notification n : dao.get(getUser()).getNotifications()) {
          json.add(jb.toJson(n));
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

}
