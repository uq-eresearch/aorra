package service.filestore;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import service.filestore.FileStore.Permission;

public class JsonBuilder {

  public JsonBuilder() {}

  public ArrayNode toJson(final Iterable<FileStore.Folder> folders)
      throws RepositoryException {
    return toJson(folders, true);
  }

  public ArrayNode toJson(final Iterable<FileStore.Folder> folders, boolean hideParents)
      throws RepositoryException {
    final ArrayNode l = JsonNodeFactory.instance.arrayNode();
    for (final FileStore.Folder folder : folders) {
      l.addAll(toJson(folder, hideParents));
    }
    return l;
  }

  public ArrayNode toJson(final FileStore.Folder folder)
      throws RepositoryException {
    return toJson(folder, true);
  }

  public ArrayNode toJson(final FileStore.Folder folder, boolean hideParent)
      throws RepositoryException {
    final ArrayNode l = JsonNodeFactory.instance.arrayNode();
    l.add(toJsonShallow(folder, hideParent));
    l.addAll(toJson(folder.getFolders(), false));
    for (final FileStore.File childFile : folder.getFiles()) {
      l.add(toJsonShallow(childFile));
    }
    return l;
  }


  public ObjectNode toJsonShallow(final FileStore.Folder folder)
      throws RepositoryException {
    return toJsonShallow(folder, false);
  }

  public ObjectNode toJsonShallow(
        final FileStore.Folder folder,
        boolean hideParent)
      throws RepositoryException {
    final ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("id", folder.getIdentifier());
    json.put("name", folder.getName());
    json.put("path", folder.getPath());
    json.put("type", "folder");
    if (folder.getParent() != null && !hideParent) {
      json.put("parent", folder.getParent().getIdentifier());
    }
    json.put("accessLevel", folder.getAccessLevel().toString());
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
    json.put("accessLevel", file.getAccessLevel().toString());
    return json;
  }

}
