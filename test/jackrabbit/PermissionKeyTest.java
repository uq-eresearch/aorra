package jackrabbit;

import static org.fest.assertions.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class PermissionKeyTest {

  @Test
  public void testEqualsWorks() {
    final PermissionKey pk = new PermissionKey("default", "test", "1234");
    assertThat(pk).isEqualTo(new PermissionKey("default", "test", "1234"));
    assertThat(pk).isEqualTo(pk);
    assertThat(pk).isNotEqualTo(null);
    for (PermissionKey otherPk : getOtherPKs(pk)) {
      assertThat(pk).isNotEqualTo(otherPk);
      assertThat(otherPk).isNotEqualTo(pk);
      assertThat(otherPk).isEqualTo(
          new PermissionKey(
              otherPk.getWorkspace(),
              otherPk.getPrincipal(),
              otherPk.getId()));
    }
  }

  @Test
  public void testHashCodeWorks() {
    final PermissionKey pk = new PermissionKey("default", "test", "1234");
    // Ideally, hashCode() should be different for different objects
    for (PermissionKey otherPk : getOtherPKs(pk)) {
      assertThat(pk.hashCode()).isNotEqualTo(otherPk.hashCode());
    }
    // hashCode() should be the same for equal objects
    assertThat(pk.hashCode())
      .isEqualTo(new PermissionKey("default", "test", "1234").hashCode());
  }

  @Test
  public void testToString() {
    final PermissionKey pk = new PermissionKey("default", "test", "1234");
    // toString() should be different for different objects
    for (PermissionKey otherPk : getOtherPKs(pk)) {
      assertThat(pk.toString()).isNotEqualTo(otherPk.toString());
    }
  }

  private List<PermissionKey> getOtherPKs(PermissionKey pk) {
    final String workspace = pk.getWorkspace();
    final String principal = pk.getPrincipal();
    final String id = pk.getId();
    final List<PermissionKey> list = new LinkedList<PermissionKey>();
    list.add(new PermissionKey("other", principal, id));
    list.add(new PermissionKey(workspace, "other", id));
    list.add(new PermissionKey(workspace, principal, "9999"));
    list.add(new PermissionKey(null, principal, id));
    list.add(new PermissionKey(workspace, null, id));
    list.add(new PermissionKey(workspace, principal, null));
    return list;
  }

}
