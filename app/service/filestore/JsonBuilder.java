package service.filestore;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import service.filestore.FileStore.File;
import service.filestore.FileStore.Folder;

public class JsonBuilder {

  private final FileStore fileStore;
  private final Session session;

  public JsonBuilder(FileStore fileStore, Session session) {
    this.fileStore = fileStore;
    this.session = session;
  }

  public ArrayNode tree() {
    try {
      Set<Folder> folders = fileStore.getManager(session)
          .getFolders();
      // Assemble the JSON response
      final ArrayNode json = JsonNodeFactory.instance.arrayNode();
      for (Folder folder : folders) {
        json.add(buildJson(folder, null));
      }
      return json;
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
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
