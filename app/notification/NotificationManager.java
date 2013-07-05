package notification;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import models.Flag;
import models.User;
import models.UserDAO;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.jcrom.Jcrom;

import play.Application;
import play.Logger;
import play.Play;
import play.Plugin;
import play.libs.F.Function;
import scala.Tuple2;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.EventManager;
import service.filestore.EventManager.FileStoreEvent;
import service.filestore.EventManager.FileStoreEvent.EventType;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.roles.Admin;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;
import com.google.common.collect.Sets;

public class NotificationManager extends Plugin {

    private static class NotificationRunner implements Runnable {

        private volatile boolean stopped = false;

        private String lastEventId;

        @Override
        public void run() {
            Logger.debug("Notification started");
            lastEventId = fileStore().getEventManager().getLastEventId();
            Logger.debug("last event id: "+lastEventId);
            while(!stopped) {
                try {
                     sessionFactory().inSession(new Function<Session, String>() {
                        public String apply(Session session) throws UnsupportedRepositoryOperationException, RepositoryException {
                            checkEvents(session);
                            return null;
                        }
                      });
                    Thread.sleep(1000);
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            Logger.debug("Notification stopped");
        }

        private void checkEvents(Session session) throws RepositoryException {
            Iterable<Tuple2<String, FileStoreEvent>> events = fileStore().getEventManager().getSince(lastEventId);
            for(Tuple2<String, FileStoreEvent> pair : events) {
                String eventId = pair._1;
                EventManager.FileStoreEvent event = pair._2;
                Logger.debug(String.format("got event id %s type %s event info type %s with node id %s",
                        eventId, event.type, event.info.type, event.info.id));
                lastEventId = eventId;
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
            FlagStore.Manager manager = flagStore().getManager(session);
            UserDAO userdao = new UserDAO(session, jcrom());
            for(User user : userdao.list()) {
                Flag flag = manager.getFlag(FlagStore.FlagType.WATCH, nodeId, user);
                if((flag != null) && StringUtils.isNotBlank(user.getEmail())) {
                    emails.add(user.getEmail());
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

    private NotificationRunner runner;

    private static JcrSessionFactory sessionFactory() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                                   .getInstance(JcrSessionFactory.class);
    }

    protected static FileStore fileStore() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                .getInstance(FileStore.class);
    }

    private static FlagStore flagStore() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                .getInstance(FlagStore.class);
    }

    private static Jcrom jcrom() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                .getInstance(Jcrom.class);
    }

    public NotificationManager(Application application) {
        
    }

    @Override
    public void onStart() {
        if(runner == null) {
            runner = new NotificationRunner();
            Thread t = new Thread(runner, "notifications");
            t.start();
      }
    }

    @Override
    public void onStop() {
        runner.stopped = true;
        runner = null;
    }

}
