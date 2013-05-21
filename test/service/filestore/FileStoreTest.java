package service.filestore;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Scanner;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.tika.io.IOUtils;
import org.junit.Test;

import play.Logger;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.roles.Admin;

public class FileStoreTest {

  @Test
  public void canGetRootFolder() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            Set<FileStore.Folder> folders =
                fileStore().getManager(session).getFolders();
            assertThat(folders).hasSize(1);
            FileStore.Folder folder = folders.iterator().next();
            assertThat(folder.getPath()).isEqualTo("/");
            assertThat(folder.getParent()).isNull();
            assertThat(folder.getDepth()).isEqualTo(0);
            FileStore.FileOrFolder refetchedFolder =
                fileStore().getManager(session).getFileOrFolder("/");
            assertThat(refetchedFolder).isEqualTo(folder);
            return folder.getIdentifier();
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
            final String gAdmin = "testAdmin";
            final String gNone = "testNone";
            Admin.getInstance(session).getGroup().addMember(gm.create(gAdmin));
            gm.create(gNone);
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
              throws RepositoryException, IOException {
            {
              // Check user with admin sees only one root folder
              Set<FileStore.Folder> folders =
                  fileStoreImpl.getManager(session).getFolders();
              assertThat(folders).hasSize(1);
            }
            final FileStore.Folder rootFolder =
                fileStoreImpl.getManager(session).getRoot();
            final String filename = "README.txt";
            final String mimeType = "text/plain";
            final String content = "Test content.";
            try {
              final FileStore.File f = rootFolder.createFile(filename, mimeType,
                  new ByteArrayInputStream(content.getBytes()));
              // Check the parent is correct right after save
              assertThat(f.getParent().getIdentifier())
                .isEqualTo(rootFolder.getIdentifier());
              final FileStore.FileOrFolder fof = fileStoreImpl
                  .getManager(session)
                  .getFileOrFolder("/"+filename);
              assertThat(fof).isInstanceOf(FileStore.File.class);
              assertThat(fof).isEqualTo(f);
              final FileStoreImpl.File file = (FileStoreImpl.File) fof;
              assertThat(file.getIdentifier()).isNotNull();
              assertThat(file.getName()).isEqualTo(filename);
              assertThat(file.getPath()).isEqualTo("/"+filename);
              assertThat(file.getDepth()).isEqualTo(1);
              assertThat(file.getMimeType()).isEqualTo(mimeType);
              // Check the parent is correct on retrieval
              assertThat(file.getParent().getIdentifier())
                .isEqualTo(rootFolder.getIdentifier());
              assertThat(IOUtils.toString(file.getData()))
                .isEqualTo(content);
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
