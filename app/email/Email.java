package email;

import models.User;

public interface Email {

  public void sendHtmlEmail(User user, String subject, String body);

}
