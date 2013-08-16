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
import java.io.InputStream;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.junit.Test;

import com.google.common.io.ByteStreams;

import play.Logger;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.FileStore.Folder;
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
            FileStore.Manager fm = fileStore().getManager(session);
            Set<FileStore.Folder> folders =
                fileStore().getManager(session).getFolders();
            assertThat(folders).hasSize(1);
            FileStore.Folder folder = folders.iterator().next();
            assertThat(folder.getPath()).isEqualTo("/");
            assertThat(folder.getParent()).isNull();
            assertThat(folder.getDepth()).isEqualTo(0);
            assertThat(fm.getRoot().getIdentifier())
              .isEqualTo(folder.getIdentifier());
            // Following the removal of identifier-based equality, this is no
            // longer the case.
            //assertThat(fm.getByIdentifier(folder.getIdentifier()))
            //  .isEqualTo(folder);
            return folder.getIdentifier();
          }
        });
      }
    });
  }

  @Test
  public void equality() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            FileStore.Manager fm = fileStore().getManager(session);
            final FileStore.Folder folder = fm.getRoot().createFolder("test");
            final FileStore.File file = folder.createFile("test",
                "text/plain",
                new ByteArrayInputStream("Hello World!".getBytes()));
            // Check folder equality
            assertThat(folder).isEqualTo(folder);
            assertThat(folder).isNotEqualTo(null);
            assertThat(folder).isNotEqualTo(file);
            assertThat(fm.getRoot()).isNotEqualTo(folder);
            assertThat(fm.getRoot()).isEqualTo(fm.getRoot());
            // Check file equality
            assertThat(file).isEqualTo(file);
            assertThat(file).isNotEqualTo(null);
            assertThat(file).isNotEqualTo(folder);
            assertThat(file).isEqualTo(folder.getFileOrFolder("test"));
            // Versions are different to files
            assertThat(file).isNotEqualTo(file.getLatestVersion());
            return folder.getIdentifier();
          }
        });
      }
    });
  }

  @Test
  public void parentKnowsOfChildren() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws RepositoryException {
            FileStore.Manager fm = fileStore().getManager(session);
            final FileStore.Folder folder = fm.getRoot().createFolder("test");
            assertThat(folder.getFiles()).isEmpty();
            {
              final FileStore.File file = folder.createFile("test1",
                  "text/plain",
                  new ByteArrayInputStream("Hello World!".getBytes()));
              assertThat(file.getParent()).isEqualTo(folder);
              assertThat(folder.getFiles()).isNotEmpty();
              assertThat(folder.getFiles()).containsOnly(file);
            }
            {
              final FileStore.File file = folder.createFile("test2",
                  "text/plain",
                  new ByteArrayInputStream("Hello World!".getBytes()));
              assertThat(file.getParent()).isEqualTo(folder);
              assertThat(folder.getFiles()).hasSize(2);
              assertThat(folder.getFiles()).contains(file);
            }
            return session;
          }
        });
      }
    });
  }

  @Test
  public void noImplicitOverwriting() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws RepositoryException {
            final FileStore.Manager fm = fileStore().getManager(session);
            final FileStore.Folder rootFolder = fm.getRoot();
            // Test overwritting folder
            {
              final FileStore.Folder folder = rootFolder.createFolder("test");
              try {
                rootFolder.createFolder("test");
                fail("Should have thrown ItemExistsException.");
              } catch (ItemExistsException e) {
                // All good
              }
              try {
                rootFolder.createFile("test", "text/plain",
                  new ByteArrayInputStream("Hello World!".getBytes()));
                fail("Should have thrown ItemExistsException.");
              } catch (ItemExistsException e) {
                // All good
              }
              folder.delete();
            }
            // Test overwritting file
            {
              final FileStore.File file =
                rootFolder.createFile("test", "text/plain",
                    new ByteArrayInputStream("Hello World!".getBytes()));
              try {
                rootFolder.createFolder("test");
                fail("Should have thrown ItemExistsException.");
              } catch (ItemExistsException e) {
                // All good
              }
              try {
                rootFolder.createFile("test", "text/plain",
                  new ByteArrayInputStream("Hello World!".getBytes()));
                fail("Should have thrown ItemExistsException.");
              } catch (ItemExistsException e) {
                // All good
              }
              file.delete();
            }
            // Test overwritting file by rename
            {
              final FileStore.File file1 =
                  rootFolder.createFile("test", "text/plain",
                      new ByteArrayInputStream("Hello World!".getBytes()));
              final FileStore.File file2 =
                  rootFolder.createFile("rename.me", "text/plain",
                      new ByteArrayInputStream("Hello World!".getBytes()));
              assertThat(rootFolder.getFiles()).hasSize(2);
              assertThat(file1.getParent()).isEqualTo(rootFolder);
              assertThat(file2.getParent()).isEqualTo(rootFolder);
              try {
                file2.rename("test");
                fail("Should have thrown ItemExistsException.");
              } catch (ItemExistsException e) {
                // All good
              }
              assertThat(fm.getByIdentifier(file2.getIdentifier()).getName())
                .isEqualTo("rename.me");
            }
            return session;
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
    running(fakeAorraApp(true), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = sessionFactory();
        final FileStore fileStoreImpl = fileStore();
        final String userId = sessionFactory.inSession(
            new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
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
              final FileStore.Manager fm = fileStoreImpl.getManager(session);
              final FileStore.File f = rootFolder.createFile(filename, mimeType,
                  new ByteArrayInputStream(content.getBytes()));
              // Check the parent is correct right after save
              assertThat(f.getParent().getIdentifier())
                .isEqualTo(rootFolder.getIdentifier());
              final FileStore.FileOrFolder fof =
                  fm.getFileOrFolder("/"+filename);
              assertThat(fof).isInstanceOf(FileStore.File.class);
              assertThat(fof).isEqualTo(f);
              final FileStore.File file = (FileStore.File) fof;
              assertThat(file.getIdentifier()).isNotNull();
              assertThat(file.getName()).isEqualTo(filename);
              assertThat(file.getPath()).isEqualTo("/"+filename);
              assertThat(file.getDepth()).isEqualTo(1);
              assertThat(file.getMimeType()).isEqualTo(mimeType);
              // Check the parent is correct on retrieval
              assertThat(file.getParent().getIdentifier())
                .isEqualTo(rootFolder.getIdentifier());
              // Check the author info
              assertThat(file.getAuthor()).isNotNull();
              assertThat(file.getAuthor().getJackrabbitUserId())
                .isEqualTo(userId);
              assertThat(ByteStreams.toByteArray(file.getData()))
                .isEqualTo(content.getBytes());
              assertThat(fm.getByIdentifier(file.getIdentifier()))
                .isEqualTo(file);
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

  @Test
  public void groupPermissions() {
    running(fakeAorraApp(true), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = sessionFactory();
        final FileStore fileStoreImpl = fileStore();
        final String userId = sessionFactory.inSession(
            new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
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
        // Grant access to testGroup
        sessionFactory.inSession(new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            final GroupManager gm = new GroupManager(session);
            final String g = "testGroup";
            gm.create(g);
            session.save();
            final Folder rootFolder = fileStoreImpl.getManager(session).getRoot();
            // Assign new user to admin group
            gm.addMember(g, userId);
            final FileStore.Folder testFolder =
                rootFolder.createFolder("test");
            testFolder.grantAccess(g, FileStore.Permission.RW);
            return userId;
          }
        });
        // Check granted access
        sessionFactory.inSession(userId, new Function<Session,FileStore.Folder>() {
          @Override
          public FileStore.Folder apply(Session session)
              throws RepositoryException, IOException {
            final Set<FileStore.Folder> folders =
                fileStoreImpl.getManager(session).getFolders();
            assertThat(folders).hasSize(1);
            final FileStore.Folder folder = folders.iterator().next();
            assertThat(folder.getPath()).isEqualTo("/test");
            return folder;
          }
        });
        // Revoke access to test group
        sessionFactory.inSession(new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException {
            final FileStore.Folder testFolder = (FileStore.Folder)
                fileStoreImpl.getManager(session).getFileOrFolder("/test");
            testFolder.revokeAccess("testGroup");
            return userId;
          }
        });
        // Check revoked access
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
      }
    });
  }

  @Test
  public void fileVersioning() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final FileStore fileStoreImpl = fileStore();
        final String userId = getAdminUser(sessionFactory());
        sessionFactory().inSession(userId, new Function<Session,String>() {
          @Override
          public String apply(Session session) throws RepositoryException, IOException {
            FileStore.Folder folder =
                fileStoreImpl.getManager(session).getRoot();
            final String filename = "README.txt";
            final String mimeType = "text/plain";
            int i;
            FileStore.File f = null;
            for (i = 1; i <= 10; i++) {
              final InputStream data =
                  new ByteArrayInputStream(getSomeContent(i).getBytes());
              if (f == null) {
                folder.createFile(filename, mimeType, data);
              } else {
                f.update(mimeType, data);
              }
              f = (FileStore.File) fileStoreImpl
                  .getManager(session)
                  .getFileOrFolder("/"+filename);
              assertThat(ByteStreams.toByteArray(f.getData()))
                .isEqualTo(getSomeContent(i).getBytes());
              assertThat(f.getVersions().size()).isEqualTo(i);
            }
            f = (FileStore.File) fileStoreImpl
                .getManager(session)
                .getFileOrFolder("/"+filename);
            final SortedMap<String, FileStore.File> versions = f.getVersions();
            i = 1;
            for (final String versionName : versions.keySet()) {
              final FileStore.File v = versions.get(versionName);
              assertThat(versionName).isEqualTo(String.format("1.%d", i-1));
              assertThat(ByteStreams.toByteArray(v.getData()))
                .isEqualTo(getSomeContent(i).getBytes());
              i++;
            }
            return folder.getIdentifier();
          }
        });
      }

      protected String getSomeContent(int seed) {
        return String.format("Test content, version #%d.", seed);
      }

    });
  }

  public String getAdminUser(JcrSessionFactory sessionFactory) {
    return sessionFactory.inSession(new Function<Session,String>() {
      @Override
      public String apply(Session session) throws RepositoryException {
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
        final GroupManager gm = new GroupManager(session);
        Admin.getInstance(session).getGroup().addMember(gm.create("testAdmin"));
        gm.addMember("testAdmin", userId);
        return userId;
      }
    });
  }

}
