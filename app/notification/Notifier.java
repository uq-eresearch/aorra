package notification;

import javax.jcr.RepositoryException;

import models.Notification;
import service.OrderedEvent;
import service.EventManager.Event;

public interface Notifier {

  void handleEvent(OrderedEvent oe);

  public static class Events {

    public static Event create(Notification notification)
        throws RepositoryException {
      return new Event("create", nodeInfo(notification));
    }

    public static Event update(Notification notification)
        throws RepositoryException {
      return new Event("update", nodeInfo(notification));
    }

    public static Event delete(Notification notification)
        throws RepositoryException {
      return new Event("delete", nodeInfo(notification));
    }

    private static Event.NodeInfo nodeInfo(Notification notification) {
      return new Event.NodeInfo("notification", notification.getId());
    }

  }

}
