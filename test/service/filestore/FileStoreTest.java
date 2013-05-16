package service.filestore;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;
import java.util.Scanner;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.jackrabbit.api.security.user.Group;
import org.junit.Test;

import play.Logger;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.roles.Admin;

public class FileStoreTest {
  @Test
  public void canGetFolders() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new Function<Session,FileStore.Folder>() {
          @Override
          public FileStore.Folder apply(Session session) {
            try {
              Set<FileStore.Folder> folders =
                  fileStore().getManager(session).getFolders();
              assertThat(folders).hasSize(1);
              FileStore.Folder folder = folders.iterator().next();
              assertThat(folder.getPath()).isEqualTo("/");
              return folder;
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    });
  }

  @Test
  public void permissionOrder() {
    assertThat(FileStore.Permission.RW.isAtLeast(FileStore.Permission.RW))
      .isTrue();
    assertThat(FileStore.Permission.RW.isAtLeast(FileStore.Permission.RO))
      .isTrue();
    assertThat(FileStore.Permission.RO.isAtLeast(FileStore.Permission.RO))
      .isTrue();
    assertThat(FileStore.Permission.RO.isAtLeast(FileStore.Permission.RW))
      .isFalse();
    assertThat(FileStore.Permission.NONE.isAtLeast(FileStore.Permission.RW))
      .isFalse();
    assertThat(FileStore.Permission.NONE.isAtLeast(FileStore.Permission.RO))
      .isFalse();
  }

  @Test
  public void adminGroupPermissions() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = sessionFactory();
        final FileStore fileStoreImpl = fileStore();
        final String userId = sessionFactory.inSession(
            new Function<Session,String>() {
          @Override
          public String apply(Session session) {
            // Create new user
            final String userId;
            UserDAO dao = new UserDAO(session, jcrom());
            User user = new User();
            user.setEmail("user@example.com");
            user.setName("Test User");
            user = dao.create(user);
            String token = user.createVerificationToken();
            user.checkVerificationToken(token);
            dao.setPassword(user, "password");
            userId = user.getJackrabbitUserId();
            return userId;
          }
        });
        sessionFactory.inSession(userId, new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            // Unprivileged users should see no folders
            Set<FileStore.Folder> folders =
                fileStoreImpl.getManager(session).getFolders();
            assertThat(folders).hasSize(0);
            return null;
          }
        });
        sessionFactory.inSession(new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            final GroupManager gm = new GroupManager(session);
            final Group gAdmin = gm.create("testAdmin");
            final Group gNone = gm.create("testNone");
            Admin.getInstance(session).getGroup().addMember(gAdmin);
            session.save();
            final FileStoreImpl.Folder rootFolder = (FileStoreImpl.Folder)
                fileStoreImpl.getManager(session).getRoot();
            assertThat(rootFolder.getGroupPermissions().containsKey(gAdmin))
                .as("created admin group has a listed permission")
                .isTrue();
            assertThat(rootFolder.getGroupPermissions().containsKey(gNone))
                .as("created unprivileged group has a listed permission")
                .isTrue();
            assertThat(rootFolder.getGroupPermissions().get(gAdmin))
                .as("created admin group has RW permissions")
                .isEqualTo(FileStore.Permission.RW);
            assertThat(rootFolder.getGroupPermissions().get(gNone))
                .as("created unprivileged group has no permissions")
                .isEqualTo(FileStore.Permission.NONE);
            // Assign new user to admin group
            gm.addMember("testAdmin", userId);
            return userId;
          }
        });
        sessionFactory.inSession(userId, new Function<Session,FileStore.Folder>() {
          @Override
          public FileStore.Folder apply(Session session)
              throws RepositoryException {
            final FileStore.Folder rootFolder =
                fileStoreImpl.getManager(session).getRoot();
            final String filename = "README.txt";
            final String mimeType = "text/plain";
            final String content = "Test content.";
            try {
              rootFolder.createFile(filename, mimeType,
                  new ByteArrayInputStream(content.getBytes()));
              final FileStoreImpl.File file = (FileStoreImpl.File) fileStoreImpl
                  .getManager(session)
                  .getFileOrFolder("/"+filename);
              assertThat(file.getName()).isEqualTo(filename);
              assertThat(file.getMimeType()).isEqualTo(mimeType);
              Scanner scanner = new Scanner(file.getData());
              assertThat(scanner.useDelimiter("\\Z").next())
                .isEqualTo(content);
              scanner.close();
            } catch (AccessDeniedException ade) {
              Logger.debug("Access unexpectedly denied.", ade);
              fail("An admin user should be able to create a file.");
            }
            return rootFolder;
          }
        });
      }
    });
  }

}
