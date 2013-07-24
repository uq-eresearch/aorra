package models;

import static org.junit.Assert.fail;
import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;
import static test.AorraTestUtils.sessionFactory;

import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static test.AorraTestUtils.fakeAorraApp;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Test;

import com.google.common.collect.Maps;

import play.libs.F;

public class GroupManagerTest {

  @Test
  public void canCreateFindListDelete() {
    running(fakeAorraApp(), new SessionRunner() {
      @Override
      public void run(Session session) {
        String groupName = "Foo Bar";
        GroupManager gm = new GroupManager(session);
        Group group = null;
        try {
          group = gm.create(groupName);
          save(session);
        } catch (AuthorizableExistsException e) {
          fail(groupName+" should not exist.");
        }
        try {
          assertThat(gm.find(groupName)).isEqualTo(group);
        } catch (PathNotFoundException e) {
          throw new AssertionError(e);
        }
        assertThat(gm.list()).containsOnly(group);
        try {
          gm.delete(groupName);
          save(session);
        } catch (PathNotFoundException e) {
          throw new AssertionError(e);
        }
        assertThat(gm.list()).isEmpty();
      }
    });
  }

  @Test
  public void findErrors() {
    running(fakeAorraApp(), new SessionRunner() {
      @Override
      public void run(Session session) {
        GroupManager gm = new GroupManager(session);
        try {
          gm.find("doesnotexist");
          save(session);
          fail("Should trigger PathNotFoundException.");
        } catch (PathNotFoundException e) {
          assertThat(e.getMessage()).contains("not exist");
        }
        try {
          gm.find("anonymous");
          save(session);
          fail("Should trigger PathNotFoundException.");
        } catch (PathNotFoundException e) {
          assertThat(e.getMessage()).contains("not a group");
        }
        try {
          gm.find("filestoreAdmin");
          save(session);
          fail("Should trigger PathNotFoundException.");
        } catch (PathNotFoundException e) {
          assertThat(e.getMessage()).contains("not a managed group");
        }
      }
    });
  }

  @Test
  public void cannotCreateDuplicateGroups() {
    running(fakeAorraApp(), new SessionRunner() {
      @Override
      public void run(Session session) {
        String groupName = "Foo Bar";
        GroupManager gm = new GroupManager(session);
        try {
          gm.create(groupName);
          save(session);
        } catch (AuthorizableExistsException e) {
          fail(groupName+" should not exist.");
        }
        try {
          gm.create(groupName);
          save(session);
          fail(groupName+" should exist.");
        } catch (AuthorizableExistsException e) {
          // All good
        }
      }
    });
  }

  @Test
  public void canAddRemoveMembers() {
    running(fakeAorraApp(), new SessionRunner() {
      @Override
      public void run(Session session) {
        GroupManager gm = new GroupManager(session);
        final Map<String, Group> groups = Maps.newHashMap();
        for (String groupName : new String[]{"G", "a", "b"}) {
          try {
            groups.put(groupName, gm.create(groupName));
            save(session);
          } catch (AuthorizableExistsException e) {
            fail(groupName+" should not exist.");
          }
        }
        // Add some members
        try {
          gm.addMember("G", "a");
          gm.addMember("G", "b");
        } catch (PathNotFoundException e) {
          e.printStackTrace();
          fail(e.getMessage());
        }
        assertThat(gm.memberships("a")).contains(groups.get("G"));
        assertThat(gm.memberships("b")).contains(groups.get("G"));
        // Check using non-existent member
        try {
          gm.addMember("G", "doesnotexist");
          fail("Should have triggered PathNotFoundException");
        } catch (PathNotFoundException e) {
          // All good
        }
        // Remove member
        try {
          gm.removeMember("G", "a");
          assertThat(gm.memberships("a")).excludes(groups.get("G"));
          assertThat(gm.memberships("b")).contains(groups.get("G"));
        } catch (PathNotFoundException e) {
          e.printStackTrace();
          fail(e.getMessage());
        }
        // Check using non-existent member
        try {
          gm.removeMember("G", "doesnotexist");
          fail("Should have triggered PathNotFoundException");
        } catch (PathNotFoundException e) {
          // All good
        }
      }
    });
  }

  private abstract class SessionRunner implements Runnable {
    @Override
    public void run() {
      sessionFactory().inSession(new F.Function<Session, Session>() {
        @Override
        public Session apply(Session session) {
          run(session);
          return session;
        }
      });
    }

    protected abstract void run(Session session);

    protected void save(Session session) {
      try {
        session.save();
      } catch (RepositoryException e) {
        throw new AssertionError(e);
      }
    }
  }

}
