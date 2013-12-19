package notification;

import java.util.Date;

public interface EmailNotifier {

  void sendEmailNotifications(Date since, Date until);

}
