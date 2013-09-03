package notification;

import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Flag;
import models.Notification;
import models.NotificationDAO;
import models.User;
import models.UserDAO;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.jcrom.Jcrom;

import play.Application;
import play.Logger;
import play.api.templates.Html;
import play.libs.F;
import service.JcrSessionFactory;
import service.filestore.EventManager;
import service.filestore.EventManager.Event;
import service.filestore.EventManager.Event.EventType;
import service.filestore.EventManager.EventReceiver;
import service.filestore.EventManager.EventReceiverMessage;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.OrderedEvent;
import akka.actor.TypedActor;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class NotifierImpl implements Notifier, TypedActor.PreStart {

  private final JcrSessionFactory sessionFactory;
  private final FileStore fileStore;
  private final FlagStore flagStore;
  private final Jcrom jcrom;

  @Inject
  public NotifierImpl(Application application,
      JcrSessionFactory sessionFactory, FileStore fileStore,
      FlagStore flagStore, Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.fileStore = fileStore;
    this.flagStore = flagStore;
    this.jcrom = jcrom;
    updateUserModels();
  }

  // Handle old users without notifications by resaving the model
  private void updateUserModels() {
    Logger.debug("Updating user models");
    sessionFactory.inSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) {
        UserDAO dao = new UserDAO(session, jcrom);
        for (User u : dao.list())
          dao.update(u);
        return session;
      }
    });
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
        if (oe.event().type == EventType.OUTOFDATE)
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
    return event.info != null
        && event.info.type == EventManager.Event.NodeType.NOTIFICATION;
  }

  private static boolean isFlagEvent(final Event event) {
    return event.info != null
        && event.info.type == EventManager.Event.NodeType.FLAG;
  }

  private void processEvent(Session session, Event event)
      throws RepositoryException {
    if (isFlagEvent(event)) {
      if (isEditFlag(session, event)) {
        sendEditNotification(session,
            getWatchUsers(session, getTargetId(session, event)),
            event);
      }
    } else {
      sendNotification(session, event);
    }
  }

  private Iterable<User> getWatchUsers(Session session, String nodeId) {
    final List<User> users = Lists.newLinkedList();
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
    for (final User user : getWatchUsers(session, event.info.id)) {
      FileStore.FileOrFolder item = getItem(manager, event);
      final String message = views.html.notification.notification.render(event,
          item).toString();
      sendNotification(session, user, message);
    }
  }

  private FileStore.FileOrFolder getItem(
      FileStore.Manager manager, Event event) throws RepositoryException {
    return manager.getByIdentifier(event.info.id);
  }

  private String getTargetId(Session session, Event event) {
    Flag flag = flagStore.getManager(session).getFlag(event.info.id);
    return flag.getTargetId();
  }

  private boolean isEditFlag(Session session, Event event) {
    final FlagStore.Manager mgr = flagStore.getManager(session);
    final Flag f = mgr.getFlag(FlagStore.FlagType.EDIT, event.info.id);
    return f != null;
  }

  private void sendEditNotification(Session session, final Iterable<User> users,
      final Event event) throws RepositoryException {
    final Flag flag = flagStore.getManager(session).getFlag(event.info.id);
    final User creator = flag.getUser();
    final FileStore.Manager manager = fileStore.getManager(session);
    FileStore.FileOrFolder fof = manager.getByIdentifier(flag.getTargetId());
    Html msg = views.html.notification.edit_notification.render(creator, fof);
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
    fileStore.getEventManager().tell(EventManager.Event.create(notification));
  }

}