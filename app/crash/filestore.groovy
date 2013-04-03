import static play.Play.application

import java.util.SortedSet

import javax.jcr.Session

import com.google.common.collect.ImmutableSortedSet

import org.apache.jackrabbit.api.security.user.Group
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import models.GroupManager
import service.filestore.roles.Admin
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import securesocial.core.UserId

@Usage("Group information")
class filestore {

  @Usage("make group filestore administrators")
  @Command
  String makeadmin(
      @Usage("group name")
      @Argument
      String name) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          try {
            def group = (new GroupManager(session)).find(name)
            Admin.getInstance(session).getGroup().addMember(group)
          } catch (RuntimeException e) {
            return e.getMessage()
          }
          name + " granted admin access."
        }
      })
  }

  @Usage("strip admin access from this group")
  @Command
  String removeadmin(
      @Usage("group name")
      @Argument
      String name) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          try {
            def group = (new GroupManager(session)).find(name)
            Admin.getInstance(session).getGroup().removeMember(group)
          } catch (RuntimeException e) {
            return e.getMessage()
          }
          name + " stripped of admin access."
        }
      })
  }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }

}