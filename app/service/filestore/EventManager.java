package service.filestore;

import javax.jcr.RepositoryException;

import models.Flag;
import models.Notification;

public interface EventManager {

  public abstract String getLastEventId();

  public abstract Iterable<OrderedEvent> getSince(String eventId);

  public abstract void tell(EventReceiverMessage message);

  public abstract void tell(Event event);

  public interface EventReceiver {
    void push(OrderedEvent oe);
    void end();
    void end(Throwable e);
  }

  public static class EventReceiverMessage {

    public static enum MessageType {
      ADD, REMOVE
    }

    public final MessageType type;
    public final EventReceiver er;
    public final String lastId;

    protected EventReceiverMessage(final MessageType type,
        final EventReceiver er, final String lastId) {
      this.type = type;
      this.er = er;
      this.lastId = lastId;
    }

    public static EventReceiverMessage add(EventReceiver er,
        String lastId) {
      return new EventReceiverMessage(MessageType.ADD, er, lastId);
    }

    public static EventReceiverMessage remove(EventReceiver er) {
      return new EventReceiverMessage(MessageType.REMOVE, er, null);
    }

  }

  public static class Event {

    public static enum EventType {
      CREATE, UPDATE, DELETE, OUTOFDATE;
      @Override
      public String toString() { return super.toString().toLowerCase(); }
    }

    public static enum NodeType {
      FILE, FOLDER, FLAG, NOTIFICATION;
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

      NodeInfo(Notification notification) {
        this.type = NodeType.NOTIFICATION;
        this.id = notification.getId();
      }

      NodeInfo(Flag flag) {
        this.type = NodeType.FLAG;
        this.id = flag.getId();
      }

      @Override
      public String toString() {
        return String.format("%s (%s)", id, type);
      }

    }

    public final EventType type;
    public final NodeInfo info;

    protected Event(EventType type) {
      this.type = type;
      this.info = null;
    }

    protected Event(EventType type, NodeInfo info)
        throws RepositoryException {
      this.type = type;
      this.info = info;
    }

    public static Event outOfDate() {
      return new Event(EventType.OUTOFDATE);
    }

    public static Event create(FileStore.File file)
        throws RepositoryException {
      return new Event(EventType.CREATE, new NodeInfo(file));
    }

    public static Event create(FileStore.Folder folder)
        throws RepositoryException {
      return new Event(EventType.CREATE, new NodeInfo(folder));
    }

    public static Event create(Flag flag)
        throws RepositoryException {
      return new Event(EventType.CREATE, new NodeInfo(flag));
    }

    public static Event create(Notification notification)
        throws RepositoryException {
      return new Event(EventType.CREATE, new NodeInfo(notification));
    }

    public static Event updateFolder(String folderId)
        throws RepositoryException {
      return new Event(EventType.UPDATE,
          new NodeInfo(NodeType.FOLDER, folderId));
    }

    public static Event update(FileStore.File file)
        throws RepositoryException {
      return new Event(EventType.UPDATE, new NodeInfo(file));
    }

    public static Event update(Notification notification)
        throws RepositoryException {
      return new Event(EventType.UPDATE, new NodeInfo(notification));
    }

    public static Event delete(FileStore.File file)
        throws RepositoryException {
      return new Event(EventType.DELETE, new NodeInfo(file));
    }

    public static Event delete(FileStore.Folder folder)
        throws RepositoryException {
      return new Event(EventType.DELETE, new NodeInfo(folder));
    }

    public static Event delete(Flag flag)
        throws RepositoryException {
      return new Event(EventType.DELETE, new NodeInfo(flag));
    }

    public static Event delete(Notification notification)
        throws RepositoryException {
      return new Event(EventType.DELETE, new NodeInfo(notification));
    }

    @Override
    public String toString() {
      return String.format("[%s] %s", type, info);
    }

  }

}