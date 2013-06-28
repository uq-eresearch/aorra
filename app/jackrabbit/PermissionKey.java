package jackrabbit;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import jackrabbit.PermissionKey;

public final class PermissionKey {

    private final String workspace;
    private final String principal;
    private final String id;

    public PermissionKey(String workspace, String principal, String id) {
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
      return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
      return String.format("workspace %s, principal %s, itemid %s", workspace, principal, id);
    }
}
