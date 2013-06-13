package jackrabbit;

import jackrabbit.PermissionKey;

public class PermissionKey {

    private String workspace;
    private String principal;
    private String id;

    public PermissionKey(String workspace, String principal, String id) {
        super();
        this.workspace = workspace;
        this.principal = principal;
        this.id = id;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result
                + ((principal == null) ? 0 : principal.hashCode());
        result = prime * result
                + ((workspace == null) ? 0 : workspace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PermissionKey other = (PermissionKey) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (principal == null) {
            if (other.principal != null)
                return false;
        } else if (!principal.equals(other.principal))
            return false;
        if (workspace == null) {
            if (other.workspace != null)
                return false;
        } else if (!workspace.equals(other.workspace))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("workspace %s, principal %s, itemid %s", workspace, principal, id);
    }
}
