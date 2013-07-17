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
            FlagStore.Manager mgr = flagStore().getManager(session);
            String targetId = toId(session, path);
            def dao = new UserDAO(session, jcrom())
            User user = dao.findByEmail(email);
            mgr.setFlag(FlagStore.FlagType.WATCH, targetId, user);
          }
        })
    }

    @Command
    void unwatch(@Argument String path, @Argument String email) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            FlagStore.Manager mgr = flagStore().getManager(session);
            String targetId = toId(session, path);
            def dao = new UserDAO(session, jcrom())
            User user = dao.findByEmail(email);
            Flag flag = mgr.getFlag(FlagStore.FlagType.WATCH, targetId, user);
            mgr.unsetFlag(FlagStore.FlagType.WATCH, flag.getId());
          }
        })
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
