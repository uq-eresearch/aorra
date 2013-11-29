package notification;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.google.common.collect.ImmutableMap;

import models.Notification;
import service.OrderedEvent;
import service.EventManager.Event;

public interface Notifier {

  void handleEvent(OrderedEvent oe);

  public static class Events {

    public static Event create(Notification notification)
        throws RepositoryException {
      return new Event("notification:create", nodeInfo(notification));
    }

    public static Event update(Notification notification)
        throws RepositoryException {
      return new Event("notification:update", nodeInfo(notification));
    }

    public static Event delete(Notification notification)
        throws RepositoryException {
      return new Event("notification:delete", nodeInfo(notification));
    }

    private static Map<String, String> nodeInfo(Notification notification) {
      return ImmutableMap.of("id", notification.getId());
    }

  }

}
