package jackrabbit;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.CompareToBuilder;

public final class PermissionKey implements Comparable<PermissionKey> {

    private final String workspace;
    private final String principal;
    private final String id;

    private final int hashCode;

    public PermissionKey(String workspace, String principal, String id) {
      this.workspace = workspace;
      this.principal = principal;
      this.id = id;
      // Hash code will be used *very* often, and will never change
      this.hashCode = generateHashCode();
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
      return hashCode;
    }

    private int generateHashCode() {
      return (new HashCodeBuilder(17,23))
        .append(workspace)
        .append(principal)
        .append(id)
        .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
      return String.format("workspace %s, principal %s, itemid %s", workspace, principal, id);
    }

    @Override
    public int compareTo(PermissionKey o) {
      return new CompareToBuilder()
        .append(getWorkspace(), o.getWorkspace())
        .append(getPrincipal(), o.getPrincipal())
        .append(getId(), o.getId())
        .build();
    }
}
