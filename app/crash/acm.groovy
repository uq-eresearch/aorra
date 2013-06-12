import static play.Play.application

import java.security.Principal;
import java.util.SortedSet

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
//import org.apache.jackrabbit.core.security.authorization.AccessControlEntryImpl
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;

import aorra.jackrabbit.PermissionKey;
import aorra.jackrabbit.Permission;

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import models.GroupManager
import service.filestore.FileStore
import service.filestore.FileStore.Folder
import service.filestore.roles.Admin
import service.GuiceInjectionPlugin
import service.JcrSessionFactory

@Usage("Access Control Management operations")
class acm {

/*
    @Usage("show supported privileges for node")
    @Command
    void supported(@Usage("path to node") @Argument String path) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            Privilege[] privs = session.getAccessControlManager().getSupportedPrivileges(path);
            for(Privilege p : privs) {
                out.println(p.getName());
            }
          }
        })
    }

    @Usage("privileges the session has for absolute path")
    @Command
    void privileges(@Usage("path to node") @Argument String path) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            Privilege[] privs = session.getAccessControlManager().getPrivileges(path);
            for(Privilege p : privs) {
                out.println(p.getName());
            }
          }
        })
    }

    @Usage("list of access control policies that are capable of being applied to the node at path")
    @Command
    void policies(@Usage("path to node") @Argument String path) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            final JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(acm, path);
            out.println("size of AccessControlEntries in ACL: "+acl.getAccessControlEntries().length);
            for(AccessControlEntry ace : acl.getAccessControlEntries()) {
                out.println(ace.getClass().getName());
                out.println("principal: "+ ace.getPrincipal().getName());
                out.println("isAllow: " + ((JackrabbitAccessControlEntry)ace).isAllow());
                for(Privilege p : ace.getPrivileges()) {
                    out.println(p.getName());
                }
            }
          }
        })
    }

    @Usage("add privilege to group at node")
    @Command
    void addPrivilege(@Usage("path to node") @Argument String path,
     @Usage("name of group") @Argument String groupName,
     @Usage("name of privilege") @Argument String privilegeName,
     @Usage("deny this privilege") @Option(names=["d","deny"]) Boolean deny) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            try {
                final AccessControlManager acm = session.getAccessControlManager();
                final GroupManager groupManager = new GroupManager(session);
                Group group = groupManager.find(groupName);
                Principal p = group.getPrincipal();
                out.println("found group "+p.getName());
                Privilege privilege = acm.privilegeFromName(privilegeName);
                out.println(privilege.getName());
                final JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(acm, path);
                out.println("size of AccessControlEntries in ACL: "+acl.getAccessControlEntries().length);
                Privilege[] pa = new Privilege[1];
                pa[0] = privilege;
                acl.addEntry(p, pa, deny == null);
                acm.setPolicy(path, acl);
              } catch(Exception e) {
                e.printStackTrace(out);
              }
          }
        })
     }

    @Usage("remove the AccessControlEntry for a group from node")
    @Command
    void removeAce(@Usage("path to node") @Argument String path,
     @Usage("name of group") @Argument String groupName) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            try {
                final AccessControlManager acm = session.getAccessControlManager();
                final GroupManager groupManager = new GroupManager(session);
                Group group = groupManager.find(groupName);
                Principal p = group.getPrincipal();
                out.println("found group "+p.getName());
                final JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(acm, path);
                for(AccessControlEntry ace : acl.getAccessControlEntries()) {
                    if(ace.getPrincipal().getName().equals(p.getName())) {
                        acl.removeAccessControlEntry(ace);
                        acm.setPolicy(path, acl);
                        out.println("removed AccessControlEntry");
                        return;
                    }
                }
              } catch(Exception e) {
                e.printStackTrace(out);
              }
          }
        })
     }

    AccessControlEntry getAce(JackrabbitAccessControlList acl, Principal p) {
        for(AccessControlEntry ace : acl.getAccessControlEntries()) {
            if(ace.getPrincipal().getName().equals(p.getName())) {
                return ace;
            }
        }
        return null;
    }
*/
    @Usage("show AccessControlManager class name")
    @Command
    void classname() {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            out.println(acm.getClass().getName());
          }
        })
     }

    @Usage("show node id from a given path")
    @Command
    void getid(@Usage("path to node") @Argument String path) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            out.println(acm.getId(path));
            
          }
        })
     }

    @Usage("show path of a given node id")
    @Command
    void getpath(@Usage("node id to resolve to path") @Argument String id) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            out.println(acm.getPath(id));
            
          }
        })
     }


    @Usage("list permissions")
    @Command
    void list() {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            Map<PermissionKey, Permission> permissions = acm.getPermissions();
            List<PermissionKey> keys = new LinkedList<PermissionKey>(permissions.keySet());
            Collections.sort(keys, new Comparator<PermissionKey>() {
                @Override
                public int compare(PermissionKey k1, PermissionKey k2) {
                    int result = k1.getWorkspace().compareTo(k2.getWorkspace());
                    if(result == 0) {
                        result = k1.getPrincipal().compareTo(k2.getPrincipal());
                    }
                    if(result == 0) {
                        result = k1.getId().compareTo(k2.getId());
                    }
                    return result;
                }
            });
            for(PermissionKey key : keys) {
                Permission p = permissions.get(key);
                out.println(String.format("%s - %s - %s granted %s", key.getWorkspace(), key.getPrincipal(), acm.getPath(key.getId()), p.toString()));
            }
          }
        })
     }

    @Usage("grant permission to a path")
    @Command
    void grant(
        @Usage("principal") @Argument String principal, 
        @Usage("node path to grant permission to") @Argument String path,
        @Usage("permission (none, ro or rw, rw is default)") @Argument String permission,
        @Usage("name of workspace (omit for default)") @Option(names=["w","workspace"]) String workspace) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            String id = acm.getId(path);
            if(StringUtils.isBlank(permission)) {
                permission = "rw";
            }
            Permission p = Permission.valueOf(StringUtils.upperCase(permission));
            acm.grant(workspace, principal, id, p);
          }
        })
     }

    @Usage("grant permission to a path")
    @Command
    void revoke(
        @Usage("principal") @Argument String principal,
        @Usage("node path to revoke permission to") @Argument String path,
        @Usage("name of workspace (omit for default)") @Option(names=["w","workspace"]) String workspace) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            final AccessControlManager acm = session.getAccessControlManager();
            String id = acm.getId(path);
            acm.revoke(workspace, principal, id);
          }
        })
     }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }
}
