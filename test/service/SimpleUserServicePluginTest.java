package service;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.feth.play.module.pa.user.EmailIdentity;

import providers.JackrabbitEmailPasswordAuthProvider.LoginUser;

public class SimpleUserServicePluginTest {

  @Test
  public void testSave() {
    final SimpleUserServicePlugin plugin = new SimpleUserServicePlugin(null);
    {
      final AuthUser user = new LoginUser("password", "user@example.test");
      assertThat(plugin.save(user)).isEqualTo(user);
    }
    try {
      final AuthUser user = new AuthUser() {
        private static final long serialVersionUID = 1L;
        @Override
        public String getProvider() {
          return "unknown";
        }
        @Override
        public String getId() {
          return UUID.randomUUID().toString();
        }
      };
      plugin.save(user);
      fail("Should have triggered exception.");
    } catch (UnsupportedOperationException e) {
      // Good
    }
  }

  @Test
  public void testGetLocalIdentity() {
    final SimpleUserServicePlugin plugin = new SimpleUserServicePlugin(null);
    {
      final AuthUserIdentity identity = new AuthUserIdentity() {
        @Override
        public String getId() {
          return "user@example.test";
        }
        @Override
        public String getProvider() {
          return "password";
        }
      };
      final EmailIdentity emailIdentity = (EmailIdentity)
          plugin.getLocalIdentity(identity);
      assertThat(emailIdentity.getId()).isEqualTo(identity.getId());
      assertThat(emailIdentity.getProvider()).isEqualTo(identity.getProvider());
      assertThat(emailIdentity.getEmail()).isEqualTo(identity.getId());
    }
    try {
      final AuthUserIdentity identity = new AuthUserIdentity() {
        @Override
        public String getId() {
          return UUID.randomUUID().toString();
        }
        @Override
        public String getProvider() {
          return "unknown";
        }
      };
      plugin.getLocalIdentity(identity);
      fail("Should have triggered exception.");
    } catch (UnsupportedOperationException e) {
      // Good
    }
  }

  @Test
  public void testMerge() {
    final SimpleUserServicePlugin plugin = new SimpleUserServicePlugin(null);
    // Not implemented, but this stub behaviour should be safe
    final AuthUser oldUser = new LoginUser("password", "old@example.test");
    final AuthUser newUser = new LoginUser("password", "new@example.test");
    assertThat(plugin.merge(newUser, oldUser)).isEqualTo(newUser);
  }

  @Test
  public void testLink() {
    final SimpleUserServicePlugin plugin = new SimpleUserServicePlugin(null);
    // Not implemented, but this stub behaviour should be safe
    final AuthUser oldUser = new LoginUser("password", "old@example.test");
    final AuthUser newUser = new LoginUser("password", "new@example.test");
    assertThat(plugin.link(oldUser, newUser)).isEqualTo(newUser);
  }

}
