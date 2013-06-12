package service.filestore;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import service.filestore.FileStore.File;
import service.filestore.FileStore.Folder;

public class JsonBuilder {

  public JsonBuilder() {}

  public ArrayNode toJson(final Iterable<FileStore.Folder> folders)
      throws RepositoryException {
    final ArrayNode l = JsonNodeFactory.instance.arrayNode();
    for (final FileStore.Folder folder : folders) {
      l.addAll(toJson(folder));
    }
    return l;
  }

  public ArrayNode toJson(final FileStore.Folder folder)
      throws RepositoryException {
    final ArrayNode l = JsonNodeFactory.instance.arrayNode();
    l.add(toJsonShallow(folder));
    l.addAll(toJson(folder.getFolders()));
    for (final FileStore.File childFile : folder.getFiles()) {
      l.add(toJsonShallow(childFile));
    }
    return l;
  }

  public ObjectNode toJsonShallow(final FileStore.Folder folder)
      throws RepositoryException {
    final ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("id", folder.getIdentifier());
    json.put("name", folder.getName());
    json.put("path", folder.getPath());
    json.put("type", "folder");
    if (folder.getParent() != null) {
      json.put("parent", folder.getParent().getIdentifier());
    }
    return json;
  }

  public ObjectNode toJsonShallow(final FileStore.File file)
      throws RepositoryException {
    final ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("id", file.getIdentifier());
    json.put("name", file.getName());
    json.put("path", file.getPath());
    json.put("mime", file.getMimeType());
    json.put("type", "file");
    if (file.getParent() != null) {
      json.put("parent", file.getParent().getIdentifier());
    }
    return json;
  }

  public ArrayNode tree(final Iterable<FileStore.Folder> folders) {
    try {
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
