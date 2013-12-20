package notification;

import java.util.Date;

import models.User;

public interface EmailNotifier {

  void sendEmailNotifications(Date since, Date until);

  void sendEmailNotifications(User user, Date since, Date until);

}
