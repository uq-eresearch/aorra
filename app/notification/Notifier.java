package notification;

import javax.jcr.RepositoryException;

import models.Notification;
import service.OrderedEvent;
import service.EventManager.Event;
import service.EventManager.Event.EventType;
import service.EventManager.Event.NodeType;

public interface Notifier {

  void handleEvent(OrderedEvent oe);

  public static class Events {

    public static Event create(Notification notification)
        throws RepositoryException {
      return new Event(EventType.CREATE, nodeInfo(notification));
    }

    public static Event update(Notification notification)
        throws RepositoryException {
      return new Event(EventType.UPDATE, nodeInfo(notification));
    }

    public static Event delete(Notification notification)
        throws RepositoryException {
      return new Event(EventType.DELETE, nodeInfo(notification));
    }

    private static Event.NodeInfo nodeInfo(Notification notification) {
      return new Event.NodeInfo(NodeType.NOTIFICATION, notification.getId());
    }

  }

}
