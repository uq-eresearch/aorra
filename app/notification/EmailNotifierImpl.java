package notification;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.Notification;
import models.User;
import models.UserDAO;

import org.apache.commons.lang3.StringUtils;
import org.jcrom.Jcrom;

import play.Logger;
import play.libs.F;
import service.JcrSessionFactory;

import com.google.inject.Inject;

import email.Email;

public class EmailNotifierImpl implements EmailNotifier {

  private JcrSessionFactory sessionFactory;

  private Jcrom jcrom;

  private Email email;

  @Inject
  public EmailNotifierImpl(JcrSessionFactory sessionFactory, Jcrom jcrom, Email email) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
    this.email = email;
  }

  @Override
  public void sendEmailNotifications(final Date since, final Date until) {
    sessionFactory.inSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) throws RepositoryException {
        sendEmailNotifications(session, since, until);
        return session;
      }
    });
  }

  private void sendEmailNotifications(Session session, Date since, Date until) {
    UserDAO userDao = new UserDAO(session, jcrom);
    for(User user : userDao.list()) {
      String body = emailBody(user, since, until);
      if(body != null) {
        try {
          email.sendHtmlEmail(user, "AORRA notification summary", body);
        } catch(Exception e) {
          Logger.warn(String.format(
              "send notification email to user %s failed", user.getEmail()), e);
        }
      }
    }
  }

  private String emailBody(User user, Date since, Date until) {
    boolean first = true;
    StringBuilder b = new StringBuilder();
    for(Notification n : notifications(user)) {
      if(n.getCreated() != null &&
          n.getCreated().after(since) &&
          n.getCreated().before(until)) {
        if(first) {
          first = false;
        } else {
          b.append("<hr style=\"border-top: 1px solid #999999;\">");
        }
        b.append("<p><span style=\"color: #999999;font-size: 12px;\">");
        b.append(new SimpleDateFormat("EEEE, dd MMM M yyyy @ hh:mm a").
            format(n.getCreated()).replace("AM", "am").replace("PM","pm"));
        b.append("</span></p>");
        b.append(n.getMessage());
      }
    }
    if(StringUtils.isNotBlank(b.toString())) {
      b.insert(0, "<body style=\"font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif;" +
          "font-size: 14px;\">" +
          "<h3>The following changes were recently made on your watched AORRA files</h3>");
      b.append("</body>");
      return b.toString();
    } else {
      return null;
    }
  }

  private List<Notification> notifications(User user) {
    return user.getNotifications()!=null?
        user.getNotifications():Collections.<Notification>emptyList();
  }

}
