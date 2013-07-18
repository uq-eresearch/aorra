package notification;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Flag;
import models.User;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;

import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.api.mvc.Call;
import play.libs.F.Function;
import scala.Tuple2;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.EventManager;
import service.filestore.EventManager.Event;
import service.filestore.EventManager.Event.EventType;
import service.filestore.FileStore;
import service.filestore.FlagStore;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

public class NotificationManager extends Plugin {

  public static final String WAIT_MILLIS_KEY = "notifications.waitMillis";

    private static class NotificationRunner implements Runnable {

        private final long maxHoldMs;

        private volatile boolean stopped = false;

        private String lastEventId;

        private final List<Pair<Long, Event>> events = Lists.newArrayList();

        private final Application application;
        private final JcrSessionFactory sessionFactory;
        private final FileStore fileStore;
        private final FlagStore flagStore;

        @Inject
        public NotificationRunner(
            Application application,
            JcrSessionFactory sessionFactory,
            FileStore fileStore,
            FlagStore flagStore) {
          this.application = application;
          this.sessionFactory = sessionFactory;
          this.fileStore = fileStore;
          this.flagStore = flagStore;
          this.maxHoldMs =
              application.configuration().getLong(WAIT_MILLIS_KEY, 15000L);
        }

        @Override
        public void run() {
            Logger.debug("Notification started");
            while(!stopped) {
                try {
                    final List<Event> events = getEvents();
                    if(!stopped && !events.isEmpty()) {
                        sessionFactory.inSession(new Function<Session, String>() {
                            @Override
                            public String apply(Session session)
                                throws RepositoryException {
                                processEvents(session, events);
                                return null;
                            }
                          });
                    }
                } catch(Throwable t) {
                    t.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
            Logger.debug("Notification stopped");
        }

        private List<Event> getEvents() {
            List<Event> result = Lists.newArrayList();
            if(lastEventId == null) {
                lastEventId = fileStore.getEventManager().getLastEventId();
            }
            List<Event> newEvents = getNewEvents();
            long now = System.currentTimeMillis();
            for(Event event : newEvents) {
                events.add(Pair.of(now, event));
            }
            Iterator<Pair<Long, Event>> iter = events.iterator();
            List<Event> fileStoreEvents = Lists.newArrayList();
            long oldest = Long.MAX_VALUE;
            while(iter.hasNext()) {
                Pair<Long,Event> p = iter.next();
                long time = p.getLeft();
                Event event = p.getRight();
                if(event.info.type == EventManager.Event.NodeType.FLAG) {
                    result.add(event);
                    iter.remove();
                } else {
                    fileStoreEvents.add(event);
                    oldest = Math.min(oldest, time);
                }
            }
            if(!fileStoreEvents.isEmpty() && ((oldest + maxHoldMs) < now)) {
                events.clear();
                result.addAll(fileStoreEvents);
            }
            return result;
        }

        private List<Event> getNewEvents() {
            List<Event> result = Lists.newArrayList();
            Iterable<Tuple2<String, Event>> events = fileStore.getEventManager().getSince(lastEventId);
            for(Tuple2<String, Event> pair : events) {
                String eventId = pair._1;
                lastEventId = eventId;
                EventManager.Event event = pair._2;
                result.add(event);
                Logger.debug(String.format("got event id %s type %s event info type %s with node id %s",
                        eventId, event.type, event.info.type, event.info.id));
            }
            return result;
        }

        private void processEvents(Session session, List<Event> events) throws RepositoryException {
            List<Event> filestoreEvents = Lists.newArrayList();
            for(Event event : events) {
                if(stopped) {
                    break;
                }
                if(event.info.type == EventManager.Event.NodeType.FLAG && isEditFlag(session, event)) {
                    Set<String> emails = getWatchEmails(session, getTargetId(session, event));
                    sendEditNotification(session, emails, event);
                } else {
                    filestoreEvents.add(event);
                }
            }
            sendNotification(session, filestoreEvents);
        }

        private Set<String> getWatchEmails(Session session, String nodeId) {
            Set<String> emails = Sets.newHashSet();
            FlagStore.Manager manager = flagStore.getManager(session);
            Set<Flag> flags = manager.getFlags(FlagStore.FlagType.WATCH);
            for(Flag flag : flags) {
                if(flag.getTargetId().equals(nodeId)) {
                  emails.add(getEmailAddress(flag.getUser()));
                } else {
                    try {
                        if(((SessionImpl)session).getHierarchyManager().isAncestor(
                                new NodeId(flag.getTargetId()), new NodeId(nodeId))) {
                          emails.add(getEmailAddress(flag.getUser()));
                        }
                    } catch(Exception e) {}
                }
            }
            return emails;
        }

        private String getEmailAddress(User u) {
          return String.format("%s <%s>", u.getName(), u.getEmail());
        }

        private void sendNotification(Session session,
                final List<Event> events) throws RepositoryException {
            FileStore.Manager manager = fileStore.getManager(session);
            Map<String, List<Event>> notifications = Maps.newHashMap();
            for(Event event : events) {
                Set<String> emails = getWatchEmails(session, event.info.id);
                for(String email : emails) {
                    List<Event> eList = notifications.get(email);
                    if(eList == null) {
                        eList = Lists.newArrayList();
                        notifications.put(email, eList);
                    }
                    eList.add(event);
                }
            }
            for(final String email : notifications.keySet()) {
                final List<Event> e = notifications.get(email);
                Map<String, FileStore.FileOrFolder> items =
                    getItems(manager, e);
                final Body body = new Body(views.txt.email.notification.render(
                    e, items).toString());
                Logger.debug("Sending notification email to "+email);
                Mailer.getDefaultMailer().sendMail("AORRA notification", body, email);
            }
        }

        private Map<String, FileStore.FileOrFolder> getItems(
            FileStore.Manager manager, List<Event> events)
                throws RepositoryException {
          final Map<String, FileStore.FileOrFolder> m = Maps.newHashMap();
          for (final Event event : events) {
            final String fofId = event.info.id;
            if (!m.containsKey(fofId))
              m.put(fofId, manager.getByIdentifier(fofId));
          }
          return m;
        }

        private String getTargetId(Session session, Event event) {
            try {
                Flag flag = flagStore.getManager(session).getFlag(event.info.id);
                return flag.getTargetId();
            } catch(Exception e) {
                return null;
            }
        }

        private boolean isEditFlag(Session session, Event event) {
            try {
                FlagStore.Manager mgr = flagStore.getManager(session);
                Flag flag = mgr.getFlag(event.info.id);
                for(Flag f : mgr.getFlags(FlagStore.FlagType.EDIT)) {
                    if(f.getId().equals(flag.getId())) {
                        return true;
                    }
                }
                return false;
            } catch(Exception e) {
                return false;
            }
        }

        private void sendEditNotification(Session session,
                final Set<String> emails, final Event event) throws RepositoryException {
            try {
                Flag flag = flagStore.getManager(session).getFlag(event.info.id);
                String fileId = flag.getTargetId();
                User user = flag.getUser();
                FileStore.Manager manager = fileStore.getManager(session);
                FileStore.FileOrFolder ff = manager.getByIdentifier(fileId);
                if(ff == null) {
                    return;
                }
                String path = ff.getPath();
                String filetype;
                if(ff instanceof FileStore.Folder) {
                    filetype = "folder";
                } else {
                    filetype = "file";
                }
                String action;
                if(EventType.CREATE == event.type) {
                    action = "created";
                } else if(EventType.DELETE == event.type) {
                    return;
                } else if(EventType.UPDATE == event.type) {
                    return;
                } else {
                    return;
                }
                final Body body = new Body(views.txt.email.edit_notification.render(
                        path, filetype, action, user.getName()).toString());
                for(String email : emails) {
                    Mailer.getDefaultMailer().sendMail("AORRA notification", body, email);
                }
            } catch(Exception e) {
                return;
            }
        }

        private String absoluteUrl(final Call call) {
          try {
            URL baseUrl = new URL(application.configuration()
                .getString("application.baseUrl"));
            return (new URL(baseUrl, call.url())).toString();
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        }
    }

    private NotificationRunner runner;
    private final Application application;

    public NotificationManager(Application application) {
      this.application = application;
    }

    @Override
    public void onStart() {
      runner = injector().getInstance(NotificationRunner.class);
      Thread t = new Thread(runner, "notifications");
      t.start();
    }

    @Override
    public void onStop() {
      runner.stopped = true;
      runner = null;
    }

    private Injector injector() {
      return GuiceInjectionPlugin.getInjector(application)
          .createChildInjector(new Module() {
            @Override
            public void configure(Binder binder) {
              binder.bind(Application.class).toInstance(application);
            }
          });
    }

  public static String absUrl(EventManager.Event event) {
    try {
      URL baseUrl = new URL(
          Play.application().configuration().getString("application.baseUrl"));
      final Call call;
      switch (event.info.type) {
      case FILE:
        call = controllers.routes.FileStoreController.showFile(event.info.id);
        break;
      case FOLDER:
        call = controllers.routes.FileStoreController.showFolder(event.info.id);
        break;
      default:
        throw new RuntimeException(
            "Invalid event type for URL:" + event.info.type);
      }
      return (new URL(baseUrl, call.url())).toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

}
