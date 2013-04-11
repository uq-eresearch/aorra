package controllers;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import securesocial.core.java.SecureSocial;
import service.JcrSessionFactory;
import service.filestore.FileStore.Folder;
import service.filestore.FileStore.File;

import com.google.inject.Inject;

public final class FileStore extends Controller {

  private final service.filestore.FileStore fileStore;
  private final JcrSessionFactory sessionFactory;

  @Inject
  public FileStore(final JcrSessionFactory sessionFactory,
      final service.filestore.FileStore fileStore) {
    this.fileStore = fileStore;
    this.sessionFactory = sessionFactory;
  }

  @SecureSocial.SecuredAction
  public Result tree() {
    return sessionFactory.inSession(new Function<Session, Result>() {
      @Override
      public final Result apply(Session session) {
        try {
          Set<Folder> folders = fileStore.getManager(session)
              .getFolders();
          // Assemble the JSON response
          final ArrayNode json = JsonNodeFactory.instance.arrayNode();
          for (Folder folder : folders) {
            json.add(buildJson(folder, null));
          }
          return ok(json).as("application/json");
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private ObjectNode
      buildJson(Folder folder, Folder parent)
          throws RepositoryException {
    final ObjectNode folderJson = Json.newObject();
    folderJson.put("id", folder.getIdentifier());
    folderJson.put("name",
        parent == null ? folder.getPath() : folder.getName());
    folderJson.put("type", "folder");
    final ObjectNode attributes = folderJson.putObject("attributes");
    attributes.put("path", folder.getPath());
    final ArrayNode children = folderJson.putArray("children");
    for (Folder subfolder : folder.getFolders())
      children.add(buildJson(subfolder, folder));
    for (File file : folder.getFiles())
      children.add(buildJson(file));
    return folderJson;
  }

  private ObjectNode buildJson(File file) throws RepositoryException {
    final ObjectNode fileJson = Json.newObject();
    fileJson.put("id", file.getIdentifier());
    fileJson.put("name", file.getName());
    fileJson.put("type", "file");
    final ObjectNode attributes = fileJson.putObject("attributes");
    attributes.put("path", file.getPath());
    return fileJson;
  }

}