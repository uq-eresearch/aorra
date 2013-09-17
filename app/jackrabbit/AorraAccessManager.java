package jackrabbit;


import java.security.AccessControlException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AorraAccessManager implements AccessControlManager, AccessManager  {

    private AMContext ctx;

    private void out(String msg) {
        //System.out.println("XXXXXXXXXXXXXXXXXXXXX   " + msg);
    }

    @Override
    public AccessControlPolicyIterator getApplicablePolicies(String arg0)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        out("getApplicablePolicies");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(String arg0)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        out("getEffectivePolicies");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public AccessControlPolicy[] getPolicies(String arg0)
            throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        out("getPolicies");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public Privilege[] getPrivileges(String arg0) throws PathNotFoundException,
            RepositoryException {
        out("getPrivileges");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public Privilege[] getSupportedPrivileges(String arg0)
            throws PathNotFoundException, RepositoryException {
        out("getSupportedPrivileges");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public boolean hasPrivileges(String arg0, Privilege[] arg1)
            throws PathNotFoundException, RepositoryException {
        out("hasPrivileges");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public Privilege privilegeFromName(String arg0)
            throws AccessControlException, RepositoryException {
        out("privilegeFromName");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public void removePolicy(String arg0, AccessControlPolicy arg1)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, LockException, VersionException,
            RepositoryException {
        out("removePolicy");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public void setPolicy(String arg0, AccessControlPolicy arg1)
            throws PathNotFoundException, AccessControlException,
            AccessDeniedException, LockException, VersionException,
            RepositoryException {
        out("setPolicy");
        throw new NotImplementedException("not implemented");
    }

    @Override
    public boolean canAccess(String workspaceName) {
        out("canAccess");
        // grant access to all workspaces
        return true;
    }

    @Override
    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        out(String.format("canRead %s %s denotesNode:%s",
            itemPath, itemId, itemId.denotesNode()));
        Path path = ctx.getHierarchyManager().getPath(itemId);
        out("ItemId resolved to path: "+path.getString());
        Permission p = getPermission(itemId);
        out("found permission: "+p.toString());
        return p.isRead();
    }

    @Deprecated
    @Override
    public void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        out(String.format("checkPermission %s %s", id, permissions));
        if(!isGranted(id, permissions)) {
            throw new AccessDeniedException(String.format(
                    "access denied on %s with permissions %s", id, permissions));
        }
    }

    @Override
    public void checkPermission(Path absPath, int permissions)
            throws AccessDeniedException, RepositoryException {
        out(String.format("checkPermission %s %s", absPath, permissions));
        if(!isGranted(absPath, permissions)) {
            throw new AccessDeniedException(String.format(
                    "access denied on %s with permissions %s", absPath, permissions));
        }
    }

    @Override
    public void checkRepositoryPermission(int permissions)
            throws AccessDeniedException, RepositoryException {
        out("checkRepositoryPermission");
    }

    @Override
    public void close() throws Exception {
        out("close");
    }

    @Override
    public void init(AMContext context) throws AccessDeniedException, Exception {
        this.init(context, null, null);
    }

    @Override
    public void init(AMContext context, AccessControlProvider acProvider,
            WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException,
            Exception {
        out("init");
        this.ctx = context;
        Subject subject = ctx.getSubject();
        Set<Principal> p = subject.getPrincipals();
        for(Principal principal : p) {
            out("principal: "+principal.getName());
        }
        out("home dir: "+ctx.getHomeDir().getAbsolutePath());
        out("workspace: "+ctx.getWorkspaceName());
    }

    @Deprecated
    @Override
    public boolean isGranted(ItemId id, int permissions)
            throws ItemNotFoundException, RepositoryException {
        out(String.format("isGranted %s %s", id, permissions));
        Permission p = getPermission(id);
        boolean result;
        result = (permissions == org.apache.jackrabbit.core.security.authorization.Permission.READ)
                ?p.isRead():p.isWrite();
        out("isGranted: "+result);
        return result;
    }

    @Override
    public boolean isGranted(Path path, int permissions) throws RepositoryException {
        out(String.format("isGranted %s %s", path.toString(), permissions));
        ItemId id = resolvePathtoRoot(path);
        if(id == null) {
            return true;
        } else {
            return isGranted(id, permissions);
        }
    }

    @Override
    public boolean isGranted(Path parentPath, Name childName, int permissions)
            throws RepositoryException {
        out(String.format("isGranted %s %s %s", parentPath.toString(), childName, permissions));
        PathBuilder builder = new PathBuilder(parentPath);
        builder.addLast(childName);
        return isGranted(builder.getPath(), permissions);
    }

    private ItemId resolvePathtoRoot(Path path) throws RepositoryException {
        Path current = path;
        while(current != null) {
            ItemId id = resolvePath(current);
            if(id != null) {
                out(String.format("resolved path %s to id %s", current, id));
                return id;
            }
            out(String.format("could not resolve path %s to id, trying to resolve parent...", current));
            String jcrPath = ctx.getNamePathResolver().getJCRPath(current);
            out(String.format("jcr path: %s", jcrPath));
            Path qpath = ctx.getNamePathResolver().getQPath(jcrPath);
            id = resolvePath(qpath);
            if(id != null) {
                out(String.format("resolved path %s to id %s", qpath, id));
                return id;
            } else {
                out(qpath.toString());
            }
            try {
                current = current.getAncestor(1);
            } catch(PathNotFoundException e) {
                break;
            }
        }
        return null;
    }

    private ItemId resolvePath(Path path) throws RepositoryException {
        ItemId id = ctx.getHierarchyManager().resolveNodePath(path);
        if(id == null) {
            id = ctx.getHierarchyManager().resolvePropertyPath(path);
        }
        return id;
    }

    private Permission getPermission(ItemId id) throws RepositoryException {
        // system and admin principal have full access
        if(isPrincipal("system") || isPrincipal("admin")) {
            return Permission.RW;
        }
        List<Permission> pList = Lists.newArrayList();
        Subject subject = ctx.getSubject();
        Set<Principal> p = subject.getPrincipals();
        for(Principal principal : p) {
            Permission permission = getPermission(id, principal);
            if(permission != null) {
                pList.add(permission);
                //optimise: if this is a RW permission exit loop, can't get better than this
                if(Permission.RW.equals(permission)) {
                    break;
                }
            }
        }
        if(pList.isEmpty()) {
            return getDefaultPermission();
        } else {
            Collections.sort(pList);
            Permission permission = pList.get(pList.size()-1);
            if(Permission.NONE.equals(permission) && id.denotesNode()) {
                // if permission is NONE and id is a node (property can't be an ancestor see below) try to widen to RO:
                // 1. get all RO and RW permissions of subjects principals
                // 2. check if id is an ancestor of any of the ids configured in the permissions if so return RO
                for(NodeId checkId : getRwRoIdsFromPermissions()) {
                    if(ctx.getHierarchyManager().isAncestor((NodeId)id, checkId)) {
                        out(String.format("%s is ancestor of %s, granting RO", id, checkId));
                        return Permission.RO;
                    }
                }
            }
            return permission;
        }
    }

    private ItemId getParentId(ItemId id) throws RepositoryException {
        try {
            Path path = ctx.getHierarchyManager().getPath(id);
            Path parent = path.getAncestor(1);
            NodeId parentId = ctx.getHierarchyManager().resolveNodePath(parent);
            return parentId;
        } catch(PathNotFoundException e) {
            return null;
        }
    }

    private List<NodeId> getRwRoIdsFromPermissions() {
        List<NodeId> result = Lists.newArrayList();
        Subject subject = ctx.getSubject();
        Set<Principal> p = subject.getPrincipals();
        for(Principal principal : p) {
            for(Map.Entry<PermissionKey, Permission> me : PermissionStore.getInstance().getPermissions().entrySet()) {
                PermissionKey key = me.getKey();
                if(!key.getWorkspace().equals(ctx.getWorkspaceName())) {
                    continue;
                }
                if(!key.getPrincipal().equals(principal.getName())) {
                    continue;
                }
                Permission permission = me.getValue();
                if(!(Permission.RO.equals(permission) || Permission.RW.equals(permission))) {
                    continue;
                }
                NodeId id = new NodeId(key.getId());
                if(nodeExists(id)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private boolean nodeExists(NodeId id) {
        try {
            return ctx.getHierarchyManager().getPath(id) != null;
        } catch(Exception e) {
            return false;
        }
    }

    private Permission getPermission(ItemId id, Principal principal) throws RepositoryException {
        ItemId i = id;
        while(true) {
            Permission p = getItemPermission(i, principal);
            if(p!= null) {
                return p;
            }
            i = getParentId(i);
            if(i == null) {
                break;
            }
        }
        return null;
    }

    private Permission getItemPermission(ItemId id, Principal principal) {
        PermissionKey key = new PermissionKey(ctx.getWorkspaceName(), principal.getName(), id.toString());
        //out("key: "+key.toString());
        return PermissionStore.getInstance().getPermission(key);
    }

    private boolean isPrincipal(String name) {
        return getPrincipal(name) != null?true:false;
    }

    private Principal getPrincipal(String name) {
        Subject subject = ctx.getSubject();
        Set<Principal> p = subject.getPrincipals();
        for(Principal principal : p) {
            if(name.equals(principal.getName())) {
                return principal;
            }
        }
        return null;
    }

    private Permission getDefaultPermission() {
        return Permission.RW;
    }

    private ItemId makeId(String id) throws ItemNotFoundException {
      try {
        return new NodeId(id);
      } catch (IllegalArgumentException e) {}
      try {
        return PropertyId.valueOf(id);
      } catch (IllegalArgumentException e) {}
      throw new IllegalArgumentException(String.format(
          "\"%s\" is not a valid node or property ID", id));
    }

    public String getId(String path) throws RepositoryException {
        Path p = ctx.getNamePathResolver().getQPath(path);
        ItemId id = resolvePath(p);
        return id.toString();
    }

    public String getPath(String id) throws ItemNotFoundException, RepositoryException {
        Path path = ctx.getHierarchyManager().getPath(makeId(id));
        return ctx.getNamePathResolver().getJCRPath(path);
    }

    public Map<PermissionKey, Permission> getPermissions() {
        return PermissionStore.getInstance().getPermissions();
    }

    public void grant(String workspace, String principal, String id, Permission permission) throws RepositoryException {
        PermissionStore.getInstance().grant(ctx.getSession(), workspace, principal, id, permission);
    }

    public void grant(Principal principal, String path, Permission permission) throws RepositoryException {
        ItemId id = resolvePath(ctx.getNamePathResolver().getQPath(path));
        if(id != null) {
            PermissionStore.getInstance().grant(ctx.getSession(), "default", principal.getName(), id.toString(), permission);
        } else {
            throw new ItemNotFoundException(String.format("could not resolve path %s to id", path));
        }
    }

    public boolean revoke(String workspace, String principal, String id) throws RepositoryException {
        return PermissionStore.getInstance().revoke(ctx.getSession(), workspace, principal, id);
    }

    public void initStore(Session session) throws RepositoryException {
        PermissionStore.getInstance().init(session);
    }

    public Map<Principal, Permission> getPermissions(String workspace,
            String path) throws RepositoryException {
        out(String.format("getPermissions %s %s", workspace, path));
        Map<Principal, Permission> result = Maps.newHashMap();
        Path p = ctx.getNamePathResolver().getQPath(path);
        ItemId id = this.resolvePath(p);
        if(id == null) {
            return result;
        }
        for(Map.Entry<PermissionKey, Permission> me : PermissionStore.getInstance().getPermissions().entrySet()) {
            PermissionKey key = me.getKey();
            if(StringUtils.equals(workspace, key.getWorkspace()) &&
                    StringUtils.equals(id.toString(), key.getId())) {
                Principal principal = ((JackrabbitSession)ctx.getSession()).getPrincipalManager(
                        ).getPrincipal(key.getPrincipal());
                if(principal != null) {
                    result.put(principal, me.getValue());
                }
            }
        }
        return result;
    }

    public Map<Principal, Permission> getPermissions(String path) throws RepositoryException {
        return getPermissions(ctx.getWorkspaceName(), path);
    }

}
