package controllers;

import static service.filestore.roles.Admin.isAdmin;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Notification;
import models.NotificationDAO;
import models.User;
import models.UserDAO;
import notification.Notifier.Events;

import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.EventManager;
import service.JcrSessionFactory;
import service.filestore.JsonBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public final class UserController extends SessionAwareController {

  private final EventManager eventManager;

  @Inject
  UserController(JcrSessionFactory sessionFactory, Jcrom jcrom,
      CacheableUserProvider subjectHandler,
      EventManager eventManager) {
    super(sessionFactory, jcrom, subjectHandler);
    this.eventManager = eventManager;
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

  @SubjectPresent
  public Result getNotification(final String id) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final Notification n = findNotificationByID(session, id);
        if (n == null)
          return notFound();
        return ok(jb.toJson(n)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result putNotification(final String id) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final NotificationDAO dao = new NotificationDAO(session, jcrom);
        final Notification n = findNotificationByID(session, id);
        if (n == null)
          return notFound();
        final JsonNode json = ctx().request().body().asJson();
        if (!json.has("read"))
          return badRequest("Should have read attribute.");
        n.setRead(json.get("read").asBoolean());
        dao.update(n);
        eventManager.tell(Events.update(n));
        return ok(jb.toJson(n)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result deleteNotification(final String id) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final NotificationDAO dao = new NotificationDAO(session, jcrom);
        final Notification n = findNotificationByID(session, id);
        if (n == null)
          return notFound();
        dao.removeById(n.getId());
        session.save();
        eventManager.tell(Events.delete(n));
        return noContent();
      }
    });
  }

  /**
   * Inefficient but safe & simple finder for notifications by ID.
   *
   * @param session
   * @param id
   * @return
   */
  protected Notification findNotificationByID(Session session, String id) {
    final UserDAO dao = new UserDAO(session, jcrom);
    for (Notification n : dao.get(getUser()).getNotifications()) {
      if (n.getId().equals(id))
        return n;
    }
    return null;
  }

}
