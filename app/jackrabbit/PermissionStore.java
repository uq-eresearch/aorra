package jackrabbit;


import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.security.AccessControlException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

public class PermissionStore {
    
    private static final String PSTORE = "pstore";
    private static final String PSTORE_PATH = "/"+PSTORE;

    private static final String WORKSPACE = "workspace";
    private static final String PRINCIPAL = "principal";
    private static final String ITEMID = "itemid";
    private static final String PERMISSION = "permission";
    private static final String PERMISSIONS = "permissions";

    private static PermissionStore INSTANCE;

    private Map<PermissionKey, Permission> permissions = Maps.newHashMap();

    private String getProperty(Node n, String property) throws PathNotFoundException, RepositoryException {
        Property p = n.getProperty(property);
        if(p != null) {
            return p.getString();
        } else {
            return null;
        }
    }

    private void writePermissions(Session session) throws RepositoryException {
        deletePermissions(session);
        Node pNode = getPermissions(session);
        for(Map.Entry<PermissionKey, Permission> me : permissions.entrySet()) {
            Node p = pNode.addNode(PERMISSION);
            p.setProperty(WORKSPACE, me.getKey().getWorkspace());
            p.setProperty(PRINCIPAL, me.getKey().getPrincipal());
            p.setProperty(ITEMID, me.getKey().getId());
            p.setProperty(PERMISSION, me.getValue().toString());
        }
    }

    private void deletePermissions(Session session) throws RepositoryException {
        Node permissions = getPermissions(session);
        NodeIterator iter = permissions.getNodes();
        while(iter.hasNext()) {
            Node p = iter.nextNode();
            p.remove();
        }
    }

    private Node getPStore(Session session) throws RepositoryException {
        try {
            Node pstore = session.getNode(PSTORE_PATH);
            return pstore;
        } catch(PathNotFoundException e) {
            Node root = session.getRootNode();
            Node pstore = root.addNode(PSTORE);
            return pstore;
        }
    }

    private Node getPermissions(Session session) throws RepositoryException {
        Node pstore = getPStore(session);
        try {
            Node permissions = pstore.getNode(PERMISSIONS);
            return permissions;
        } catch(PathNotFoundException e) {
            Node permissions = pstore.addNode(PERMISSIONS);
            return permissions;
        }

        
    }

    public static synchronized PermissionStore getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new PermissionStore();
        }
        return INSTANCE;
    }

    public synchronized Map<PermissionKey, Permission> getPermissions() {
        return Maps.newHashMap(permissions);
    }

    public synchronized void grant(Session session, 
            String workspace, String principal, String id, Permission permission) throws RepositoryException {
        checkPermission(session);
        try {
            PermissionKey key = new PermissionKey((workspace!=null?workspace:"default"), principal, id);
            permissions.put(key, permission);
            writePermissions(session);
        } finally {
            init(session);
        }
    }

    public synchronized boolean revoke(Session session ,
            String workspace, String principal, String id) throws RepositoryException {
        checkPermission(session);
        try {
            PermissionKey key = new PermissionKey((workspace!=null?workspace:"default"), principal, id);
            Permission p = permissions.remove(key);
            writePermissions(session);
            return p != null;
        } finally {
            init(session);
        }
    }

    private void checkPermission(Session session) throws AccessControlException, RepositoryException {
        session.checkPermission(PSTORE_PATH, Session.ACTION_ADD_NODE);
    }

    public synchronized void init(Session session) throws RepositoryException {
        permissions.clear();
        try {
            Node pNode = getPermissions(session);
            NodeIterator iter = pNode.getNodes();
            while(iter.hasNext()) {
                Node n = iter.nextNode();
                try {
                    String workspace = getProperty(n, WORKSPACE);
                    String principal = getProperty(n, PRINCIPAL);
                    String id = getProperty(n, ITEMID);
                    String permission = getProperty(n, PERMISSION);
                    Permission p = Permission.valueOf(StringUtils.upperCase(permission));
                    if(isNotBlank(workspace) && isNotBlank(principal) && isNotBlank(id) && p != null) {
                        permissions.put(new PermissionKey(workspace, principal, id), p);
                    }
                } catch(PathNotFoundException e) {
                    
                }
            }
        } catch(Exception e) {
            throw new RepositoryException(e);
        }
    }

    public synchronized Permission getPermission(PermissionKey key) {
        return permissions.get(key);
    }
}