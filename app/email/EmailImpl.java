package email;

import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.Mail.Body;

import models.User;

public class EmailImpl implements Email {

  @Override
  public void sendHtmlEmail(User user, String subject, String body) {
    if(user.isVerified()) {
      final Body b = new Body(null, body);
      Mailer.getDefaultMailer().sendMail(subject, b, user.getEmail());
    }
  }

}
