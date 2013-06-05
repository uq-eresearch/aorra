package service.filestore;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.google.common.collect.ImmutableMap;

import play.Logger;
import play.api.libs.iteratee.Concurrent.Channel;
import scala.Tuple2;
import service.EventTimeline;
import service.InMemoryEventTimeline;
import akka.actor.UntypedActor;

public class EventManager extends UntypedActor {

  private final EventTimeline<String, FileStoreEvent> history =
      new InMemoryEventTimeline<FileStoreEvent>();

  private final Set<Channel<Tuple2<String, FileStoreEvent>>> channels =
      new HashSet<Channel<Tuple2<String, FileStoreEvent>>>();

  @Override
  public void onReceive(Object message) throws Exception {
    if (message instanceof FileStoreEvent) {
      final FileStoreEvent event = (FileStoreEvent) message;
      Logger.debug(this+" - Adding event to history: "+event);
      history.record(event);
      Logger.debug(this+" - Pushing event to channels: "+event);
      for (Channel<Tuple2<String, FileStoreEvent>> channel : channels) {
        // Push event ID and event
        channel.push(new Tuple2<String, FileStoreEvent>(
            history.getLastEventId(), event));
        Logger.debug(this+"- Pushed to channel: "+channel);
      }
    } else if (message instanceof ChannelMessage) {
      @SuppressWarnings("unchecked")
      ChannelMessage<Tuple2<String, FileStoreEvent>> channelMessage =
          (ChannelMessage<Tuple2<String, FileStoreEvent>>) message;
      switch (channelMessage.type) {
      case ADD:
        Logger.debug(this+" - Adding notification channel.");
        channels.add(channelMessage.channel);
        break;
      case REMOVE:
        Logger.debug(this+" - Removing notification channel.");
        channels.remove(channelMessage.channel);
        break;
      }
    } else {
      unhandled(message);
    }
  }

  public static class ChannelMessage<T> {

    private static enum MessageType { ADD, REMOVE }

    public final MessageType type;
    public final Channel<T> channel;

    protected ChannelMessage(MessageType type, Channel<T> channel) {
      this.type = type;
      this.channel = channel;
    }

    public static <T> ChannelMessage<T> add(Channel<T> channel) {
      return new ChannelMessage<T>(MessageType.ADD, channel);
    }

    public static <T> ChannelMessage<T> remove(Channel<T> channel) {
      return new ChannelMessage<T>(MessageType.REMOVE, channel);
    }

  }

  public static class FileStoreEvent {

    private static enum EventType {
      CREATE, UPDATE, DELETE;
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