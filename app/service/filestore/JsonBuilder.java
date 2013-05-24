package service.filestore;

import java.text.DateFormat;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import service.filestore.FileStore.File;
import service.filestore.FileStore.Folder;

public class JsonBuilder {

  private final FileStore fileStoreImpl;
  private final Session session;

  public JsonBuilder(FileStore fileStoreImpl, Session session) {
    this.fileStoreImpl = fileStoreImpl;
    this.session = session;
  }

  public ArrayNode tree() {
    try {
      Set<Folder> folders = fileStoreImpl.getManager(session)
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
    attributes.put("mimeType", file.getMimeType());
    attributes.put("path", file.getPath());
    if (file.getAuthor() != null) {
      attributes.put("authorEmail", file.getAuthor().getEmail());
      attributes.put("authorName", file.getAuthor().getName());
    }
    if (file.getModificationTime() != null) {
      attributes.put("lastModified",
        DateFormatUtils.ISO_DATETIME_FORMAT.format(
            file.getModificationTime()));
    }
    return fileJson;
  }
}
