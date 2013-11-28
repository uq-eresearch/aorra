package notification;

import service.OrderedEvent;

public interface Notifier {

  void handleEvent(OrderedEvent oe);

}
