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
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import securesocial.core.UserId

@Usage("Group information")
class group {

  @Usage("create new group")
  @Command
  String add(
      @Usage("group name")
      @Argument
      String name) {
    def group = sessionFactory().inSession(new Function<Session, Group>() {
        public Group apply(Session session) {
          return (new GroupManager(session)).create(name);
        }
      })
    group.toString()
  }
  
  @Usage("list existing groups")
  @Command
  String list() {
    def groups = sessionFactory().inSession(
      new Function<Session, SortedSet<String>>() {
        public SortedSet<String> apply(Session session) {
          ImmutableSortedSet.copyOf((new GroupManager(session)).list()
                            .collect{ it.getID() });
        }
      })
    groups.join("\n")
  }
  
  @Usage("delete group")
  @Command
  String remove(
      @Usage("group name")
      @Argument
      String name) {
    sessionFactory().inSession(new Function<Session, String>() {
      public String apply(Session session) {
        try {
          (new GroupManager(session)).delete(name)
        } catch (RuntimeException e) {
          return e.getCause().getMessage()
        }
        "Group successfully deleted."
      }
    })
  }
  
  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }
  
}