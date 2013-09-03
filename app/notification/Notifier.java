package notification;

import service.filestore.OrderedEvent;

public interface Notifier {

  void handleEvent(OrderedEvent oe);

}
