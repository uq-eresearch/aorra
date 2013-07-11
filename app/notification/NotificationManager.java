package notification;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Flag;
import models.User;
import models.UserDAO;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jcrom.Jcrom;

import play.Application;
import play.Logger;
import play.Plugin;
import play.libs.F.Function;
import scala.Tuple2;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.EventManager;
import service.filestore.EventManager.Event;
import service.filestore.EventManager.Event.EventType;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.roles.Admin;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class NotificationManager extends Plugin {

    private static class NotificationRunner implements Runnable {

        private volatile boolean stopped = false;

        private String lastEventId;

        private final JcrSessionFactory sessionFactory;
        private final FileStore fileStore;
        private final FlagStore flagStore;
        private final Jcrom jcrom;

        @Inject
        public NotificationRunner(
            JcrSessionFactory sessionFactory,
            FileStore fileStore,
            FlagStore flagStore,
            Jcrom jcrom) {
          this.sessionFactory = sessionFactory;
          this.fileStore = fileStore;
          this.flagStore = flagStore;
          this.jcrom = jcrom;
        }

        @Override
        public void run() {
            Logger.debug("Notification started");
            lastEventId = fileStore.getEventManager().getLastEventId();
            Logger.debug("last event id: "+lastEventId);
            while(!stopped) {
                try {
                    final List<Event> events = getNewEvents();
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
                    Thread.sleep(1000);
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            Logger.debug("Notification stopped");
        }

        private List<Event> getNewEvents() {
            List<Event> result = Lists.newArrayList();
            Iterable<Tuple2<String, Event>> events = fileStore.getEventManager().getSince(lastEventId);
            for(Tuple2<String, Event> pair : events) {
                String eventId = pair._1;
                EventManager.Event event = pair._2;
                Logger.debug(String.format("got event id %s type %s event info type %s with node id %s",
                        eventId, event.type, event.info.type, event.info.id));
                lastEventId = eventId;
                result.add(event);
            }
            return result;
        }

        private void processEvents(Session session, List<Event> events) throws RepositoryException {
            for(Event event : events) {
                if(stopped) {
                    break;
                }
                Set<String> emails = getFileStoreAdminEmails(session, null, null);
                emails.addAll(getWatchEmails(session, event.info.id));
                sendMail(session, emails, event);
            }
        }

        private Set<String> getFileStoreAdminEmails(Session session,
                Group group, Set<String> emails) throws RepositoryException {
            if(group == null) {
                emails = Sets.newHashSet();
                group = Admin.getInstance(session).getGroup();
            }
            Iterator<Authorizable> aIter = group.getDeclaredMembers();
            while(aIter.hasNext()) {
                Authorizable authorizable = aIter.next();
                if(authorizable instanceof Group) {
                    getFileStoreAdminEmails(session, (Group)authorizable, emails);
                } else {
                    String id = authorizable.getID();
                    try {
                        // Get user email if user
                        Node node = session.getNodeByIdentifier(id);
                        emails.add(node.getProperty("email").getValue().getString());
                      } catch (Exception e) {}
                }
            }
            return emails;
        }

        private Set<String> getWatchEmails(Session session, String nodeId) {
            Set<String> emails = Sets.newHashSet();
            FlagStore.Manager manager = flagStore.getManager(session);
            UserDAO userdao = new UserDAO(session, jcrom);
            for(User user : userdao.list()) {
                Flag flag = manager.getFlag(FlagStore.FlagType.WATCH, nodeId, user);
                if((flag != null) && StringUtils.isNotBlank(user.getEmail())) {
                    emails.add(user.getEmail());
                }
            }
            return emails;
        }

        private void sendMail(Session session,
                final Set<String> emails, final Event event) throws RepositoryException {
            FileStore.Manager manager = fileStore.getManager(session);
            String fileId = event.info.id;
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
                action = "deleted";
            } else if(EventType.UPDATE == event.type) {
                action = "updated";
            } else {
                return;
            }
            final Body body = new Body(views.txt.email.notification.render(
                    path, filetype, action).toString());
            for(String email : emails) {
                Mailer.getDefaultMailer().sendMail("AORRA notification", body, email);
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
      return GuiceInjectionPlugin.getInjector(application);
    }

}
