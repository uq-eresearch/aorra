package notification;

import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Flag;
import models.Notification;
import models.NotificationDAO;
import models.User;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.jcrom.Jcrom;

import play.api.templates.Html;
import play.libs.F;
import service.EventManager.Event;
import service.EventManager.EventReceiver;
import service.EventManager.EventReceiverMessage;
import service.JcrSessionFactory;
import service.OrderedEvent;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import akka.actor.TypedActor;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class NotifierImpl implements Notifier, TypedActor.PreStart {

  private final JcrSessionFactory sessionFactory;
  private final FileStore fileStore;
  private final FlagStore flagStore;
  private final Jcrom jcrom;

  @Inject
  public NotifierImpl(JcrSessionFactory sessionFactory, FileStore fileStore,
      FlagStore flagStore, Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.fileStore = fileStore;
    this.flagStore = flagStore;
    this.jcrom = jcrom;
  }

  @Override
  public void preStart() {
    attachEventReceiver();
  }

  protected void attachEventReceiver() {
    final Notifier n = TypedActor.<Notifier>self();
    final EventReceiver er = new EventReceiver() {
      @Override
      public void end() {}

      @Override
      public void end(Throwable e) {}

      @Override
      public void push(OrderedEvent oe) {
        // Skip out-of-date messages
        if (oe.event().type.equals("outofdate"))
          return;
        // Trigger notification for event
        n.handleEvent(oe);
      }
    };
    fileStore.getEventManager().tell(EventReceiverMessage.add(er, null));
  }

  @Override
  public void handleEvent(final OrderedEvent oe) {
    if (isNotificationEvent(oe.event())) {
      // Notification events do not produce notifications! ;-)
      return;
    }
    sessionFactory.inSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) throws RepositoryException {
        processEvent(session, oe.event());
        return session;
      }
    });
  }

  private static boolean isNotificationEvent(final Event event) {
    return event.type.startsWith("notification:");
  }

  private static boolean isFlagEvent(final Event event) {
    return event.type.startsWith("flag:");
  }

  private static boolean isCommentEvent(final Event event) {
    return event.type.startsWith("comment:");
  }

  private void processEvent(Session session, Event event)
      throws RepositoryException {
    if (isFlagEvent(event)) {
      if (isEditFlag(session, event)) {
        sendEditNotification(session,
            getWatchUsers(session, event.info("target:id")),
            event);
      }
    } else if (isCommentEvent(event)) {
      sendCommentNotification(session,
          getWatchUsers(session, event.info("target:id")), event);
    } else {
      sendNotification(session, event);
    }
  }

  private Iterable<User> getWatchUsers(Session session, String nodeId) {
    final Set<User> users = Sets.newHashSet();
    FlagStore.Manager manager = flagStore.getManager(session);
    for (Flag flag : manager.getFlags(FlagStore.FlagType.WATCH)) {
      if (flag.getTargetId().equals(nodeId)) {
        users.add(flag.getUser());
      } else {
        try {
          if (((SessionImpl) session).getHierarchyManager().isAncestor(
              new NodeId(flag.getTargetId()), new NodeId(nodeId))) {
            users.add(flag.getUser());
          }
        } catch (RepositoryException e) {
          // Dump trace and ignore
          e.printStackTrace();
        }
      }
    }
    return users;
  }

  private void sendNotification(Session session, final Event event)
      throws RepositoryException {
    final FileStore.Manager manager = fileStore.getManager(session);
    for (final User user : getWatchUsers(session, event.info("id"))) {
      FileStore.FileOrFolder item = getItem(manager, event);
      final String message = views.html.notification.notification.render(event,
          item).toString();
      sendNotification(session, user, message);
    }
  }

  private FileStore.FileOrFolder getItem(
      FileStore.Manager manager, Event event) throws RepositoryException {
    return manager.getByIdentifier(event.info("id"));
  }

  private boolean isEditFlag(Session session, Event event) {
    FlagStore.FlagType t = FlagStore.FlagType.valueOf(event.info("type"));
    return t == FlagStore.FlagType.EDIT;
  }

  private void sendEditNotification(final Session session,
      final Iterable<User> users,
      final Event event) throws RepositoryException {
    final FileStore.Manager manager = fileStore.getManager(session);
    final FileStore.FileOrFolder fof =
        manager.getByIdentifier(event.info("target:id"));
    final Html msg;
    if (event.type.endsWith("create")) {
      msg = views.html.notification.editFlagCreated.render(event, fof);
    } else if (event.type.endsWith("delete")) {
      msg = views.html.notification.editFlagRemoved.render(event, fof);
    } else {
      return;
    }
    for (User u : users) {
      sendNotification(session, u, msg.toString());
    }
  }

  private void sendCommentNotification(final Session session,
      final Iterable<User> users,
      final Event event) throws RepositoryException {
    final FileStore.Manager manager = fileStore.getManager(session);
    final FileStore.FileOrFolder fof =
        manager.getByIdentifier(event.info("target:id"));
    if (fof == null) {
      // We can't issue a useful comment notification
      return;
    }
    final Html msg = views.html.notification.comment.render(event, fof);
    for (User u : users) {
      sendNotification(session, u, msg.toString());
    }
  }

  private void sendNotification(Session session, User to, String msg)
      throws ItemNotFoundException, RepositoryException {
    NotificationDAO notificationDao = new NotificationDAO(session, jcrom);
    Notification notification = new Notification(to, msg);
    notificationDao.create(notification);
    session.save();
    fileStore.getEventManager().tell(Events.create(notification));
  }
}