package service.filestore;

import static com.google.common.collect.Lists.newLinkedList;
import static org.fest.assertions.Assertions.assertThat;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.injector;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

import javax.jcr.Session;

import models.User;

import org.junit.Test;

import play.libs.F;
import play.test.FakeRequest;

public class CommentStoreTest {

  @Test
  public void createComment() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest req)
          throws Throwable {
        final CommentStore cs = injector().getInstance(CommentStore.class);
        final CommentStore.Manager csm = cs.getManager(session);
        final String targetId = UUID.randomUUID().toString();
        final String msg = "Mr. Flibble's very cross.";
        final CommentStore.Comment c1 =
          createComment(csm, user.getId(), targetId, msg);
        final CommentStore.Comment c2 =
          createComment(csm, user.getId(), targetId, msg);
        assertThat(c2).isNotEqualTo(c1);
        assertThat(c2.getId()).isNotEqualTo(c1.getId());
        return session;
      }


      public CommentStore.Comment createComment(
          final CommentStore.Manager m,
          final String userId, final String targetId, final String msg) {
        final Calendar beforeCreation = Calendar.getInstance();
        final CommentStore.Comment c = m.create(userId, targetId, msg);
        assertThat(c.getId()).isNotNull();
        assertThat(c.getUserId()).isEqualTo(userId);
        assertThat(c.getTargetId()).isEqualTo(targetId);
        assertThat(c.getMessage()).isEqualTo(msg);
        assertThat(c.getCreationTime()).isNotNull();
        assertThat(c.getModificationTime()).isNotNull();
        assertThat(c.getCreationTime().getTimeInMillis())
          .isGreaterThanOrEqualTo(beforeCreation.getTimeInMillis());
        assertThat(c.getModificationTime().getTimeInMillis())
          .isGreaterThanOrEqualTo(beforeCreation.getTimeInMillis());
        return c;
      }
    });
  }

  @Test
  public void findById() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest req)
          throws Throwable {
        final CommentStore cs = injector().getInstance(CommentStore.class);
        final CommentStore.Manager csm = cs.getManager(session);
        final String targetId = UUID.randomUUID().toString();
        final String msg = "Mr. Flibble's very cross.";
        final CommentStore.Comment c = csm.create(user.getId(), targetId, msg);
        final CommentStore.Comment foundC = csm.findById(c.getId());
        assertThat(foundC).isNotNull();
        assertThat(foundC.getId()).isEqualTo(c.getId());
        assertThat(foundC).isEqualTo(c);
        return session;
      }
    });
  }

  @Test
  public void findByTargetId() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest req)
          throws Throwable {
        final CommentStore cs = injector().getInstance(CommentStore.class);
        final CommentStore.Manager csm = cs.getManager(session);
        final LinkedList<String> targetIds = new LinkedList<String>();
        for (int i = 1; i <= 10; i++) {
          targetIds.add(UUID.randomUUID().toString());
        }
        final List<CommentStore.Comment> createdComments = newLinkedList();
        for (String targetId : targetIds) {
          for (int i = 1; i <= 10; i++) {
            createdComments.add(
                csm.create(user.getId(), targetId, "Message #"+i));
          }
        }
        final SortedSet<CommentStore.Comment> foundComments =
            csm.findByTarget(targetIds.getFirst());
        assertThat(foundComments).hasSize(10);
        for (CommentStore.Comment c : createdComments) {
          if (c.getId().equals(targetIds.getFirst())) {
            assertThat(foundComments).contains(c);
          }
        }
        return session;
      }
    });
  }

  @Test
  public void update() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest req)
          throws Throwable {
        final CommentStore cs = injector().getInstance(CommentStore.class);
        final CommentStore.Manager csm = cs.getManager(session);
        final String targetId = UUID.randomUUID().toString();
        final String msg1 = "Mr. Flibble's very cross.";
        final String msg2 = "Game over, boys.";
        final CommentStore.Comment c = csm.create(user.getId(), targetId, msg1);
        assertThat(c.getMessage()).isEqualTo(msg1);
        assertThat(c.getModificationTime()).isEqualTo(c.getCreationTime());
        c.setMessage(msg2);
        // Ensure there's a time gap
        Thread.sleep(2);
        final CommentStore.Comment updatedC = csm.update(c);
        assertThat(updatedC).isNotNull();
        assertThat(updatedC.getMessage()).isEqualTo(msg2);
        assertThat(updatedC.getModificationTime().getTimeInMillis())
          .isNotEqualTo(updatedC.getCreationTime().getTimeInMillis());
        return session;
      }
    });
  }

  @Test
  public void delete() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest req)
          throws Throwable {
        final CommentStore cs = injector().getInstance(CommentStore.class);
        final CommentStore.Manager csm = cs.getManager(session);
        final String targetId = UUID.randomUUID().toString();
        final String msg = "Mr. Flibble's very cross.";
        final CommentStore.Comment c = csm.create(user.getId(), targetId, msg);
        csm.delete(c);
        final CommentStore.Comment foundC = csm.findById(c.getId());
        assertThat(foundC).isNull();
        return session;
      }
    });
  }

}
