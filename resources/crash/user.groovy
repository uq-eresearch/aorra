import static play.Play.application;

import java.util.Date;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Argument
import org.crsh.cli.Option
import org.crsh.cli.Required
import org.jcrom.Jcrom
import models.Notification
import models.User
import models.UserDAO
import notification.EmailNotifier
import play.Play
import play.libs.F.Function
import providers.JackrabbitEmailPasswordAuthProvider
import service.GuiceInjectionPlugin
import service.JcrSessionFactory

@Usage("User information")
class user {

  @Usage("invite user")
  @Command
  String invite(
      @Usage("email")
      @Argument
      @Required
      String email,
      @Usage("name")
      @Argument
      @Required
      String name) {
    authProvider().signup(new User.Invite(email, name))
  }

  @Usage("list users")
  @Command
  String list() {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          def dao = new UserDAO(session, jcrom())
          dao.list().collect { user ->
            user.toString()
          }.join("\n")
        } catch (RuntimeException e) {
          return e.getMessage()
        }
      }
    })+ "\n";
  }

  @Usage("delete user")
  @Command
  String delete(
      @Usage("email")
      @Argument
      String email,
      @Usage("delete all?")
      @Option(names=["a","all"])
      Boolean all) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          try {
            def dao = new UserDAO(session, jcrom())
            if (all) {
              return dao.list().collect { u ->
                dao.delete(u)
                "Deleted "+u
              }.join("\n") + "\n"
            } else {
              def u = dao.findByEmail(email)
              if (u == null)
                return email + " does not exist"
              dao.delete(u)
              return "Deleted "+u+"\n"
            }
          } catch (RuntimeException e) {
            return e.getMessage()
          }
        }
      })
  }
  
  @Usage("update user node format")
  @Command
  String update() {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          def dao = new UserDAO(session, jcrom())
          dao.list().collect { user ->
            session.getNode(user.getNodePath()).addMixin(
              NodeType.MIX_REFERENCEABLE);
            user.toString()
          }.join("\n")
        } catch (RuntimeException e) {
          return e.getMessage()
        }
      }
    })+ "\n";
  }

  @Usage("suspend user access")
  @Command
  String suspend(
      @Usage("email")
      @Argument
      String email) {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          def dao = new UserDAO(session, jcrom())
          def u = dao.findByEmail(email)
          dao.suspend(u);
          return u.toString() + " is no longer verified."
        } catch (RuntimeException e) {
          return e.getMessage()
        }
      }
    })+ "\n";
  }

  @Usage("suspend user access")
  @Command
  String unsuspend(
      @Usage("email")
      @Argument
      String email) {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          def dao = new UserDAO(session, jcrom())
          def u = dao.findByEmail(email)
          if (u.isVerified()) {
            return u.toString() + " does not appear to be suspended."
          }
          dao.unsuspend(u);
          return u.toString() + " is verified again."
        } catch (RuntimeException e) {
          return e.getMessage()
        }
      }
    })+ "\n";
  }

  @Usage("show  notifications")
  @Command
  String notifications(@Usage("email") @Argument String email) {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          def dao = new UserDAO(session, jcrom())
          User user = dao.findByEmail(email);
          if(user.getNotifications() == null || user.getNotifications().isEmpty()) {
            out.println("no notifications");
          } else {
            for(Notification notification : user.getNotifications()) {
                out.println("notification id: "+notification.getId());
                out.println("notification date: "+notification.getCreated());
                out.println("notification message: "+notification.getMessage());
                out.println("notification read: "+notification.isRead());
                out.println("----------------------");
            }
          }
        } catch (RuntimeException e) {
          return e.getMessage()
        }
      }
    });
  }


  @Usage("send notification email")
  @Command
  String email(
    @Usage("email") @Argument String email,
    @Usage("notifications since dd/MM/yyyy HH:mm:ss") @Argument String since,
    @Usage("notifications until dd/MM/yyyy HH:mm:ss") @Argument String until) {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
          Date start;
          try {
            start = sdf.parse(since);
          } catch(Exception e) {
            start = new Date(0);
          }
          Date end;
          try {
            end = sdf.parse(until);
          } catch(Exception e) {
            end = new Date();
          }
          def dao = new UserDAO(session, jcrom())
          User user = dao.findByEmail(email);
          if(user == null) {
            out.println("no user with email "+email);
          } else {
            out.println("sending notification email to "+email+
                " including notifications from "+start.toString()+" until "+end.toString());
            emailNotifier().sendEmailNotifications(user, start, end);
          }
        } catch(Exception e) {
          return e.getMessage()
        }
      }
    });
  }

  private EmailNotifier emailNotifier() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(EmailNotifier.class);
  }

  private Jcrom jcrom() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(Jcrom.class);
  }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }

  private JackrabbitEmailPasswordAuthProvider authProvider() {
    return application().plugin(JackrabbitEmailPasswordAuthProvider.class)
  }

}