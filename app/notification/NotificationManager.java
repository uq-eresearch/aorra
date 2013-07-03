package notification;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

import play.Logger;
import play.Play;
import scala.Tuple2;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.EventManager;
import service.filestore.EventManager.FileStoreEvent;
import service.filestore.EventManager.FileStoreEvent.EventType;
import service.filestore.FileStore;
import service.filestore.roles.Admin;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class NotificationManager {

    private static class NotificationRunner implements Runnable {

        private EventManager eventmanager;

        private volatile boolean stopped = false;

        public NotificationRunner(EventManager eventmanager) {
            this.eventmanager = eventmanager;
        }

        @Override
        public void run() {
            Logger.debug("Notification started");
            String lastEventId = eventmanager.getLastEventId();
            Logger.debug("last event id: "+lastEventId);
            Session session;
            try {
                session = sessionFactory().newAdminSession();
            } catch(RepositoryException e) {
                throw new RuntimeException(e);
            }
            while(!stopped) {
                try {
                    Iterable<Tuple2<String, FileStoreEvent>> events = eventmanager.getSince(lastEventId);
                    for(Tuple2<String, FileStoreEvent> pair : events) {
                        String eventId = pair._1;
                        EventManager.FileStoreEvent event = pair._2;
                        Logger.debug(String.format("got event id %s type %s event info type %s with node id %s",
                                eventId, event.type, event.info.type, event.info.id));
                        lastEventId = eventId;
                        sendMail(session, getFileStoreAdminEmails(session, null, null), event);
                    }
                    Thread.sleep(1000);
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            session.logout();
            Logger.debug("Notification stopped");
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

        private void sendMail(Session session,
                final Set<String> emails, final FileStoreEvent event) throws RepositoryException {
            FileStore.Manager manager = fileStore().getManager(session);
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

    private EventManager eventmanager;

    private NotificationRunner runner;
    
    @Inject
    public NotificationManager(FileStore filestore) {
        this.eventmanager = filestore.getEventManager();
    }

    public void start() {
        if(runner == null) {
              runner = new NotificationRunner(eventmanager);
              Thread t = new Thread(runner, "notifications");
              t.start();
        }
    }

    public void stop() {
        runner.stopped = true;
        runner = null;
    }

    private static JcrSessionFactory sessionFactory() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                                   .getInstance(JcrSessionFactory.class);
    }

    protected static FileStore fileStore() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                .getInstance(FileStore.class);
    }

}
