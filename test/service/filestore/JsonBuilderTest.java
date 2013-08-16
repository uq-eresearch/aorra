package service.filestore;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

import javax.jcr.RepositoryException;

import models.Notification;
import models.User;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.util.ISO8601Utils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import service.filestore.FileStore.File;
import service.filestore.FileStore.FileOrFolder;
import service.filestore.FileStore.Permission;

public class JsonBuilderTest {

  @Test
  public void testFromRoot() throws RepositoryException {
    final JsonBuilder jb = new JsonBuilder();
    final FileStore.Folder folder = new TestFolder("filestore", "/", null);
    final FileStore.Folder subfolder = folder.createFolder("a");
    final FileStore.File file1 = folder.createFile("README.txt", "text/plain",
        new ByteArrayInputStream("This is a test file.".getBytes()));

    final ArrayNode jsonArray = jb.toJson(folder);
    {
      final JsonNode json = jsonArray.get(0);
      assertThat(json.get("id")).isNotNull();
      assertThat(json.get("id").asText()).isEqualTo(folder.getIdentifier());
      assertThat(json.get("name")).isNotNull();
      assertThat(json.get("name").asText()).isEqualTo(folder.getName());
      assertThat(json.get("path")).isNotNull();
      assertThat(json.get("path").asText()).isEqualTo(folder.getPath());
      assertThat(json.get("parent")).isNull();
      assertThat(json.get("type")).isNotNull();
      assertThat(json.get("type").asText()).isEqualTo("folder");
      assertThat(json.get("accessLevel")).isNotNull();
      assertThat(json.get("accessLevel").asText()).isEqualTo("RO");
    }
    {
      final JsonNode json = jsonArray.get(1);
      assertThat(json.get("id")).isNotNull();
      assertThat(json.get("id").asText()).isEqualTo(subfolder.getIdentifier());
      assertThat(json.get("name")).isNotNull();
      assertThat(json.get("name").asText()).isEqualTo(subfolder.getName());
      assertThat(json.get("path")).isNotNull();
      assertThat(json.get("path").asText()).isEqualTo(subfolder.getPath());
      assertThat(json.get("parent")).isNotNull();
      assertThat(json.get("parent").asText()).isEqualTo(
          subfolder.getParent().getIdentifier());
      assertThat(json.get("type")).isNotNull();
      assertThat(json.get("type").asText()).isEqualTo("folder");
      assertThat(json.get("accessLevel")).isNotNull();
      assertThat(json.get("accessLevel").asText()).isEqualTo("RO");
    }
    {
      final JsonNode json = jsonArray.get(2);
      assertThat(json.get("id")).isNotNull();
      assertThat(json.get("id").asText()).isEqualTo(file1.getIdentifier());
      assertThat(json.get("name")).isNotNull();
      assertThat(json.get("name").asText()).isEqualTo(file1.getName());
      assertThat(json.get("path")).isNotNull();
      assertThat(json.get("path").asText()).isEqualTo(file1.getPath());
      assertThat(json.get("type")).isNotNull();
      assertThat(json.get("type").asText()).isEqualTo("file");
      assertThat(json.get("parent")).isNotNull();
      assertThat(json.get("parent").asText()).isEqualTo(
          file1.getParent().getIdentifier());
      assertThat(json.get("mime")).isNotNull();
      assertThat(json.get("mime").asText()).isEqualTo(file1.getMimeType());
      assertThat(json.get("data")).isNull();
      assertThat(json.get("accessLevel")).isNotNull();
      assertThat(json.get("accessLevel").asText()).isEqualTo("RO");
    }
  }

  @Test
  public void testFromSubfolder() throws RepositoryException {
    final JsonBuilder jb = new JsonBuilder();
    final FileStore.Folder folder = new TestFolder("filestore", "/", null);
    final FileStore.Folder subfolder = folder.createFolder("a");
    folder.createFile("README.txt", "text/plain",
        new ByteArrayInputStream("This is a test file.".getBytes()));

    final ArrayNode jsonArray = jb.toJson(subfolder);
    {
      final JsonNode json = jsonArray.get(0);
      assertThat(json.get("id")).isNotNull();
      assertThat(json.get("id").asText()).isEqualTo(subfolder.getIdentifier());
      assertThat(json.get("name")).isNotNull();
      assertThat(json.get("name").asText()).isEqualTo(subfolder.getName());
      assertThat(json.get("path")).isNotNull();
      assertThat(json.get("path").asText()).isEqualTo(subfolder.getPath());
      assertThat(json.get("parent")).isNull();
      assertThat(json.get("accessLevel")).isNotNull();
      assertThat(json.get("accessLevel").asText()).isEqualTo("RO");
    }
  }

  @Test
  public void testUser() {
    final JsonBuilder jb = new JsonBuilder();
    final String randomId = UUID.randomUUID().toString();
    final User user = new User() {
      @Override
      public String getId() {
        return randomId;
      }
    };
    user.setName("Test User");
    user.setEmail("test@example.com");
    user.setVerified(true);
    // Get JSON
    final ObjectNode json = jb.toJson(user, false);
    assertThat(json.get("id")).isNotNull();
    assertThat(json.get("id").asText()).isEqualTo(user.getId());
    assertThat(json.get("name")).isNotNull();
    assertThat(json.get("name").asText()).isEqualTo(user.getName());
    assertThat(json.get("email")).isNotNull();
    assertThat(json.get("email").asText()).isEqualTo(user.getEmail());
    assertThat(json.get("isAdmin")).isNotNull();
    assertThat(json.get("isAdmin").asBoolean()).isEqualTo(false);
  }

  @Test
  public void testNotification() {
    final JsonBuilder jb = new JsonBuilder();
    final String randomId = UUID.randomUUID().toString();
    final Notification notification = new Notification() {
      @Override
      public String getId() {
        return randomId;
      }

      @Override
      public Date getCreated() {
        return new Date(0);
      }
    };
    notification.setMessage("Test message.");
    // Get JSON
    final ObjectNode json = jb.toJson(notification);
    assertThat(json.get("id")).isNotNull();
    assertThat(json.get("id").asText()).isEqualTo(notification.getId());
    assertThat(json.get("read")).isNotNull();
    assertThat(json.get("read").asBoolean()).isEqualTo(false);
    assertThat(json.get("message")).isNotNull();
    assertThat(json.get("message").asText())
      .isEqualTo(notification.getMessage());
    assertThat(json.get("timestamp")).isNotNull();
    assertThat(json.get("timestamp").asText()).isEqualTo(
        ISO8601Utils.format(notification.getCreated()));
  }


  private class TestFolder implements FileStore.Folder {

    final Map<String, FileOrFolder> children =
        new HashMap<String, FileOrFolder>();
    private final String identifier;
    private final String name;
    private final String path;
    private final FileStore.Folder parent;

    public TestFolder(
        final String name,
        final String path,
        final FileStore.Folder parent) {
      this.identifier = UUID.randomUUID().toString();
      this.name = name;
      this.path = path;
      this.parent = parent;
    }

    @Override
    public String getIdentifier() { return this.identifier; }

    @Override
    public String getName() { return this.name; }

    @Override
    public int getDepth() {
      if (parent == null) return 0;
      return 1 + parent.getDepth();
    }

    @Override
    public String getPath() { return this.path; }

    @Override
    public FileStore.Folder getParent() { return this.parent; }

    @Override
    public void delete() {
      // Do nothing
    }

    @Override
    public FileStore.Folder createFolder(String name)
        throws RepositoryException {
      final FileStore.Folder child = new TestFolder(
          name, this.getPath()+"/"+name, this);
      children.put(name, child);
      return child;
    }

    @Override
    public FileStore.File createFile(String name, String mime, InputStream data)
        throws RepositoryException {
      final FileStore.File child = new TestFile(
          name, this.getPath()+"/"+name, this, mime, data);
      children.put(name, child);
      return child;
    }

    @Override
    public FileOrFolder getFileOrFolder(String name) throws RepositoryException {
      return children.get(name);
    }

    @Override
    public Set<FileStore.File> getFiles() {
      return getChildren(FileStore.File.class);
    }

    @Override
    public Set<FileStore.Folder> getFolders() {
      return getChildren(FileStore.Folder.class);
    }

    private <T> Set<T> getChildren(Class<T> clazz) {
      final Set<T> s = new HashSet<T>();
      for (final FileStore.FileOrFolder fof : children.values()) {
        if (clazz.isInstance(fof)) {
          @SuppressWarnings("unchecked")
          final T t = (T) fof;
          s.add(t);
        }
      }
      return s;
    }


    @Override
    public Map<String, Permission> getGroupPermissions() {
      final Map<String, Permission> m = new HashMap<String, Permission>();
      m.put("a", Permission.RW);
      m.put("b", Permission.RO);
      m.put("c", Permission.NONE);
      return m;
    }

    @Override
    public void grantAccess(String groupName, Permission permission)
        throws RepositoryException {
      throw new NotImplementedException();
    }

    @Override
    public void revokeAccess(String groupName) throws RepositoryException {
      throw new NotImplementedException();
    }

    @Override
    public Permission getAccessLevel() {
      return Permission.RO;
    }

    @Override
    public void rename(String newName) throws RepositoryException {
      throw new NotImplementedException();
    }

    public void reload() {
    }

  }

  private class TestFile implements FileStore.File {

    private final String identifier;
    private final String name;
    private final String path;
    private final FileStore.Folder parent;
    private final String mime;
    private final byte[] data;
    private final Calendar modificationTime;

    public TestFile(final FileStore.File original,
        final String mime,
        final InputStream data) throws RepositoryException {
      this(original.getIdentifier(), original.getName(), original.getPath(), original.getParent(), mime, data);
    }

    public TestFile(
        final String name,
        final String path,
        final FileStore.Folder parent,
        final String mime,
        final InputStream data) {
      this(UUID.randomUUID().toString(), name, path, parent, mime, data);
    }

    private TestFile(
        final String identifier,
        final String name,
        final String path,
        final FileStore.Folder parent,
        final String mime,
        final InputStream data) {
      this.identifier = identifier;
      this.name = name;
      this.path = path;
      this.parent = parent;
      this.mime = mime;
      try {
        this.data = ByteStreams.toByteArray(data);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      this.modificationTime = Calendar.getInstance();
    }

    @Override
    public String getIdentifier() { return this.identifier; }

    @Override
    public String getName() { return this.name; }

    @Override
    public int getDepth() {
      if (parent == null) return 0;
      return 1 + parent.getDepth();
    }

    @Override
    public String getPath() { return this.path; }

    @Override
    public FileStore.Folder getParent() { return this.parent; }

    @Override
    public void delete() {
      // Do nothing
    }

    @Override
    public InputStream getData() {
      return new ByteArrayInputStream(data);
    }

    @Override
    public String getMimeType() {
      return mime;
    }

    @Override
    public SortedMap<String, FileStore.File> getVersions()
        throws RepositoryException {
      throw new NotImplementedException();
    }

    @Override
    public FileStore.File update(String mime, InputStream data)
        throws RepositoryException {
      return new TestFile(this, mime, data);
    }

    @Override
    public User getAuthor() {
      return null;
    }

    @Override
    public Calendar getModificationTime() {
      return modificationTime;
    }

    @Override
    public Permission getAccessLevel() {
      return Permission.RO;
    }

    @Override
    public File getLatestVersion() throws RepositoryException {
      throw new NotImplementedException();
    }

    @Override
    public void rename(String newName) throws RepositoryException {
      throw new NotImplementedException();
    }

  }

}
