package service.filestore;

import java.text.SimpleDateFormat;

import javax.jcr.RepositoryException;

import models.Flag;
import models.Notification;
import models.User;

import com.fasterxml.jackson.databind.util.ISO8601Utils;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

public class JsonBuilder {

  public JsonBuilder() {}

  public ObjectNode toJson(final User user, final boolean isAdmin) {
    final ObjectNode json = Json.newObject();
    json.put("id", user.getId());
    json.put("name", user.getName());
    json.put("email", user.getEmail());
    json.put("isAdmin", isAdmin);
    return json;
  }

  public ObjectNode toJson(final Notification notification) {
    final ObjectNode json = Json.newObject();
    json.put("id", notification.getId());
    json.put("message", notification.getMessage());
    json.put("read", notification.isRead());
    json.put("timestamp", ISO8601Utils.format(notification.getCreated()));
    return json;
  }

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

  public ObjectNode toJson(final Flag flag)
      throws RepositoryException {
    final ObjectNode json = Json.newObject();
    json.put("id", flag.getId());
    json.put("targetId", flag.getTargetId());
    json.put("userId", flag.getUser().getId());
    return json;
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
    final SimpleDateFormat iso8601 = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SZ");
    final ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("id", file.getIdentifier());
    json.put("name", file.getName());
    json.put("path", file.getPath());
    json.put("mime", file.getMimeType());
    json.put("type", "file");
    json.put("parent", file.getParent().getIdentifier());
    json.put("accessLevel", file.getAccessLevel().toString());
    json.put("modified", iso8601.format(file.getModificationTime().getTime()));
    return json;
  }

}
