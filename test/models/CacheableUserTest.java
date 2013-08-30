package models;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.Collections;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.Lists;

import be.objectify.deadbolt.core.models.Role;

public class CacheableUserTest {

  @Test
  public void roleMembership() {
    final User user;
    final CacheableUser cacheableUser;
    {
      final String randomUserId = UUID.randomUUID().toString();
      user = new User() {
        @Override
        public String getId() { return randomUserId; }
      };
      user.setName("Tommy Atkins");
      user.setEmail("tommy@atkins.net");
      final List<Role> roles = Lists.newLinkedList();
      roles.add(new Role() {
        public String getName() {
          return "foobar";
        }
      });
      cacheableUser = new CacheableUser("test", user, roles);
    }
    assertThat(cacheableUser.getId()).isEqualTo(user.getId());
    assertThat(cacheableUser.getIdentifier()).isEqualTo(cacheableUser.getId());
    assertThat(cacheableUser.getName()).isEqualTo(user.getName());
    assertThat(cacheableUser.getEmail()).isEqualTo(user.getEmail());
    assertThat(cacheableUser.getProvider()).isEqualTo("test");
    assertThat(cacheableUser.getRoles()).hasSize(1);
    assertThat(cacheableUser.hasRole("admin")).isFalse();
    assertThat(cacheableUser.hasRole("foobar")).isTrue();
    assertThat(cacheableUser.getPermissions()).isEmpty();
  }

}
