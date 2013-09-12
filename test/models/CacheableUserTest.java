package models;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

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
      final List<String> roles = ImmutableList.of("foobar");
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
  }

}
