package service;

import javax.jcr.RepositoryException;

import service.OrderedEvent;
import models.Flag;

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

    public static class NodeInfo {

      public final String id;

      public NodeInfo(String id) {
        this.id = id;
      }

      @Override
      public String toString() {
        return id;
      }

    }

    public final String type;
    public final NodeInfo info;

    protected Event(String type) {
      this.type = type;
      this.info = null;
    }

    public Event(String type, NodeInfo info)
        throws RepositoryException {
      this.type = type;
      this.info = info;
    }

    public static Event outOfDate() {
      return new Event("outofdate");
    }

    @Override
    public String toString() {
      return String.format("[%s] %s", type, info);
    }

  }

}