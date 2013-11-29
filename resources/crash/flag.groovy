import static play.Play.application

import java.security.Principal;
import java.util.SortedSet
import java.util.Set

import javax.jcr.nodetype.NodeType
import javax.jcr.Session
import javax.jcr.security.Privilege
import javax.jcr.security.AccessControlManager
import javax.jcr.security.AccessControlPolicy
import javax.jcr.security.AccessControlPolicyIterator
import javax.jcr.security.AccessControlList
import javax.jcr.security.AccessControlEntry

import com.google.common.collect.ImmutableSortedSet

import org.apache.commons.lang.StringUtils;

import org.apache.jackrabbit.api.security.user.Group
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;

import jackrabbit.PermissionKey;
import jackrabbit.Permission;

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import models.GroupManager
import models.Flag
import service.filestore.FileStore
import service.filestore.FileStore.Folder
import service.filestore.roles.Admin
import service.filestore.FlagStore
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import org.jcrom.Jcrom
import models.User
import models.UserDAO
import service.EventManager;

@Usage("flag management operations")
class flag {

    @Usage("list all flags")
    @Command
    void list() {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            FlagStore.Manager mgr = flagStore().getManager(session);
            Set<Flag> watchFlags = mgr.getFlags(FlagStore.FlagType.WATCH);
            for(Flag flag : watchFlags) {
                out.println("watch - " + toPath(session, flag.getTargetId()) + " - " + flag.getUser().getEmail());
            }
            Set<Flag> editFlags = mgr.getFlags(FlagStore.FlagType.EDIT);
            for(Flag flag : editFlags) {
                out.println("edit - " + toPath(session, flag.getTargetId()) + " - " + flag.getUser().getEmail());
            }
          }
        })
     }

    @Command
    void watch(@Required(true) @Usage("path to file or folder") @Argument String path,
    @Required(true) @Usage("email of the registered user") @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            setFlag(session, path, email, FlagStore.FlagType.WATCH);
          }
        })
    }

    @Command
    void edit(@Required(true) @Usage("path to file or folder") @Argument String path,
    @Required(true) @Usage("email of the registered user") @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            setFlag(session, path, email, FlagStore.FlagType.EDIT);
          }
        })
    }

    void setFlag(Session session, String path, String email, FlagStore.FlagType type) {
        FlagStore.Manager mgr = flagStore().getManager(session);
        String targetId = toId(session, path);
        def dao = new UserDAO(session, jcrom())
        User user = dao.findByEmail(email);
        Flag flag = mgr.setFlag(type, targetId, user);
        fileStore().getEventManager().tell(EventManager.Event.create(flag));
    }

    @Command
    void unwatch(@Argument String path, @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            unsetFlag(session, path, email, FlagStore.FlagType.WATCH);
          }
        })
    }

    @Command
    void unedit(@Argument String path, @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            unsetFlag(session, path, email, FlagStore.FlagType.EDIT);
          }
        })
    }

    @Command
    void unwatchall(@Usage("delete all watch flags of a user (all if omitted)")
        @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            deleteAllFlags(session, email, FlagStore.FlagType.WATCH);
          }
        })
    }

    @Command
    void uneditall(@Usage("delete all edit flags of a user (all if omitted)")
        @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            deleteAllFlags(session, email, FlagStore.FlagType.EDIT);
          }
        })
    }

    void deleteAllFlags(Session session, String email, FlagStore.FlagType type) {
        FlagStore.Manager mgr = flagStore().getManager(session);
        Set<Flag> flags = mgr.getFlags(type);
        for(Flag flag : flags) {
            if((email == null) || flag.getUser().getEmail().equals(email)) {
                unsetFlag(mgr, type, flag);
            }
        }
    }

    void unsetFlag(Session session, String path, String email, FlagStore.FlagType type) {
        FlagStore.Manager mgr = flagStore().getManager(session);
        String targetId = toId(session, path);
        def dao = new UserDAO(session, jcrom())
        User user = dao.findByEmail(email);
        Flag flag = mgr.getFlag(type, targetId, user);
        unsetFlag(mgr, type, flag);
    }

    void unsetFlag(FlagStore.Manager mgr, FlagStore.FlagType type, Flag flag) {
        mgr.unsetFlag(type, flag.getId());
        fileStore().getEventManager().tell(EventManager.Event.delete(flag));
    }

    private String toPath(Session session, String id) {
        FileStore.Manager mgr = fileStore().getManager(session);
        try {
            FileStore.FileOrFolder ff = mgr.getByIdentifier(id);
            return ff.getPath();
        } catch(Exception e) {
            return id;
        }
    }

    private String toId(Session session, String path) {
        try {
            FileStore.Manager mgr = fileStore().getManager(session);
            FileStore.FileOrFolder ff = mgr.getFileOrFolder(path);
            return ff.getIdentifier();
        } catch(Exception e) {
            return path;
        }
    }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }

  private FlagStore flagStore() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(FlagStore.class);
  }

  private FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(FileStore.class);
  }

  private Jcrom jcrom() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(Jcrom.class);
  }


}
