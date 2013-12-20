package email;

import models.User;
import play.Logger;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;

public class EmailImpl implements Email {

  @Override
  public void sendHtmlEmail(User user, String subject, String body) {
    if(user.isVerified()) {
      final Body b = new Body(null, body);
      Mailer.getDefaultMailer().sendMail(subject, b, user.getEmail());
    } else {
      Logger.debug(String.format("ignoring send email request to '%s', subject '%s'" +
          " because user is not verified", user.getEmail(), subject));
    }
  }

}
