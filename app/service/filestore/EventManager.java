package service.filestore;

import javax.jcr.RepositoryException;

import play.api.libs.iteratee.Concurrent.Channel;
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

    public static enum NodeType {
      FILE, FOLDER;
      @Override
      public String toString() { return super.toString().toLowerCase(); }
    }

    public static class NodeInfo {

      public final NodeType type;
      public final String id;

      NodeInfo(NodeType type, String id) {
        this.type = type;
        this.id = id;
      }

      NodeInfo(FileStore.Folder folder) {
        this.type = NodeType.FOLDER;
        this.id = folder.getIdentifier();
      }

      NodeInfo(FileStore.File file) {
        this.type = NodeType.FILE;
        this.id = file.getIdentifier();
      }

      @Override
      public String toString() {
        return String.format("%s (%s)", id, type);
      }

    }

    public final EventType type;
    public final NodeInfo info;

    protected FileStoreEvent(EventType type) {
      this.type = type;
      this.info = null;
    }

    protected FileStoreEvent(EventType type, NodeInfo info)
        throws RepositoryException {
      this.type = type;
      this.info = info;
    }

    public static FileStoreEvent outOfDate() {
      return new FileStoreEvent(EventType.OUTOFDATE);
    }

    public static FileStoreEvent create(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.CREATE, new NodeInfo(file));
    }

    public static FileStoreEvent create(FileStore.Folder folder)
        throws RepositoryException {
      return new FileStoreEvent(EventType.CREATE, new NodeInfo(folder));
    }

    public static FileStoreEvent updateFolder(String folderId)
        throws RepositoryException {
      return new FileStoreEvent(EventType.UPDATE,
          new NodeInfo(NodeType.FOLDER, folderId));
    }

    public static FileStoreEvent update(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.UPDATE, new NodeInfo(file));
    }

    public static FileStoreEvent delete(FileStore.File file)
        throws RepositoryException {
      return new FileStoreEvent(EventType.DELETE, new NodeInfo(file));
    }

    public static FileStoreEvent delete(FileStore.Folder folder)
        throws RepositoryException {
      return new FileStoreEvent(EventType.DELETE, new NodeInfo(folder));
    }

    @Override
    public String toString() {
      return String.format("[%s] %s", type, info);
    }

  }

}