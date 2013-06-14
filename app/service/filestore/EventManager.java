package service.filestore;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.time.DateFormatUtils;

import play.api.libs.iteratee.Concurrent.Channel;

import com.google.common.collect.ImmutableMap;

import scala.Tuple2;

public interface EventManager {

  public abstract String getLastEventId();

  public abstract Iterable<Tuple2<String, FileStoreEvent>> getSince(String eventId);

  public abstract void tell(
      ChannelMessage<Tuple2<String, FileStoreEvent>> message);

  public abstract void tell(FileStoreEvent event);

  public static class ChannelMessage<T> {

    public static enum MessageType { ADD, REMOVE }

    public final MessageType type;
    public final Channel<T> channel;
    public final String lastId;

    protected ChannelMessage(final MessageType type, final Channel<T> channel,
        final String lastId) {
      this.type = type;
      this.channel = channel;
      this.lastId = lastId;
    }

    public static <T> ChannelMessage<T> add(Channel<T> channel, String lastId) {
      return new ChannelMessage<T>(MessageType.ADD, channel, lastId);
    }

    public static <T> ChannelMessage<T> remove(Channel<T> channel) {
      return new ChannelMessage<T>(MessageType.REMOVE, channel, null);
    }

  }

  public static class FileStoreEvent {

    public static enum EventType {
      CREATE, UPDATE, DELETE, OUTOFDATE;
      @Override
      public String toString() { return super.toString().toLowerCase(); }
    }

    public static class NodeInfo {

      public static enum NodeType {
        FILE, FOLDER;
        @Override
        public String toString() { return super.toString().toLowerCase(); }
      }

      public final String id;
      public final String name;
      public final String parentId;
      public final NodeType type;
      public final Map<String, Object> attributes;

      NodeInfo(FileStore.File file) throws RepositoryException {
        this.id = file.getIdentifier();
        this.name = file.getName();
        this.parentId = file.getParent().getIdentifier();
        this.type = NodeType.FILE;
        ImmutableMap.Builder<String, Object> attrs =
            ImmutableMap.<String, Object>builder();
        attrs.put("mimeType", file.getMimeType());
        attrs.put("path", file.getPath());
        if (file.getAuthor() != null) {
          attrs.put("authorEmail", file.getAuthor().getEmail());
          attrs.put("authorName", file.getAuthor().getName());
        }
        if (file.getModificationTime() != null) {
          attrs.put("lastModified",
              DateFormatUtils.ISO_DATETIME_FORMAT.format(
                  file.getModificationTime()));
        }
        this.attributes = attrs.build();
      }

      NodeInfo(FileStore.Folder folder) throws RepositoryException {
        this.id = folder.getIdentifier();
        this.name = folder.getName();
        this.parentId = folder.getParent() == null ? null :
          folder.getParent().getIdentifier();
        this.type = NodeType.FOLDER;
        ImmutableMap.Builder<String, Object> attrs =
            ImmutableMap.<String, Object>builder();
        attrs.put("path", folder.getPath());
        this.attributes = attrs.build();
      }

      @Override
      public String toString() {
        return String.format("%s (%s: %s)", id, type, name);
      }

    }

    public final EventType type;
    public final NodeInfo info;

    protected FileStoreEvent(EventType type) {
      this.type = type;
      this.info = null;
    }

    protected FileStoreEvent(EventType type, FileStore.Folder folder)
        throws RepositoryException {
      this.type = type;
      this.info = new NodeInfo(folder);
    }

    protected FileStoreEvent(EventType type, FileStore.File file)
        throws RepositoryException {
      this.type = type;
      this.info = new NodeInfo(file);
    }

    public static FileStoreEvent outOfDate() {
      return new FileStoreEvent(EventType.OUTOFDATE);
    }

    public static FileStoreEvent create(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.CREATE, file);
    }

    public static FileStoreEvent create(FileStore.Folder folder)
        throws RepositoryException {
      return new FileStoreEvent(EventType.CREATE, folder);
    }

    public static FileStoreEvent update(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.UPDATE, file);
    }

    public static FileStoreEvent delete(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.DELETE, file);
    }

    public static FileStoreEvent delete(FileStore.Folder folder)
        throws RepositoryException {
      return new FileStoreEvent(EventType.DELETE, folder);
    }

    @Override
    public String toString() {
      return String.format("[%s] %s", type, info);
    }

  }

}