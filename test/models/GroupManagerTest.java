package models;

import static org.junit.Assert.fail;
import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static test.AorraTestUtils.fakeJavaApp;

import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Test;

import play.Play;
import play.libs.F.Function;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class GroupManagerTest {
  @Test
  public void canCreateFindListDelete() {
    running(fakeJavaApp(), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = GuiceInjectionPlugin
            .getInjector(Play.application())
            .getInstance(JcrSessionFactory.class);
        sessionFactory.inSession(new Function<Session,GroupManager>() {
          @Override
          public GroupManager apply(Session session) {
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
            return gm;
          }

          private void save(Session session) {
            try {
              session.save();
            } catch (RepositoryException e) {
              throw new AssertionError(e);
            }
          }
        });

      }
    });
  }

}
