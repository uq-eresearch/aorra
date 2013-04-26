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
import models.UserDAO
import models.GroupManager
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import org.jcrom.Jcrom

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
    group.getID().toString()
  }

  @Usage("show existing group")
  @Command
  String show(@Usage("group name") @Argument String name) {
    sessionFactory().inSession(
      new Function<Session, String>() {
        public String apply(Session session) {
          def group = (new GroupManager(session)).find(name);
          String.format("%s : %s\n", group.getID(),
            group.getMembers().collect {
              def node = session.getNodeByIdentifier(it.getID())
              if (node == null)
                it.getID()
              else
                node.getProperty("email").getValue().getString()
            }.join(", "))
        }
      })
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
    groups.join("\n")+"\n"
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
          return e.getMessage()
        }
        "Group successfully deleted.\n"
      }
    })
  }

  @Usage("add/remove users in group")
  @Command
  String members(
      @Usage("subject group")
      @Option(names=["g","group"])
      String groupName,
      @Usage("user ids or group names to add")
      @Option(names=["a","add"])
      List<String> addIds,
      @Usage("user ids or group names to remove")
      @Option(names=["d","delete"])
      List<String> removeIds) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          def dao = userDAO(session)
          def gm = new GroupManager(session)
          def addMessages = addIds.collect {
            try {
              def userId = dao.findByEmail(it).getJackrabbitUserId()
              gm.addMember(groupName, userId)
            } catch (RuntimeException e) {
              e.printStackTrace();
              return e.getMessage()
            }
            it+" successfully added to "+groupName
          }
          def removeMessages = removeIds.collect {
            try {
              def userId = dao.findByEmail(it).getJackrabbitUserId()
              gm.removeMember(groupName, userId)
            } catch (RuntimeException e) {
              return e.getMessage()
            }
            it+" successfully removed from "+groupName
          }
          (addMessages + removeMessages).join("\n")+"\n"
        }
      })
  }
  
  private UserDAO userDAO(session) {
    return new UserDAO(session, jcrom());
  }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }
  
  private Jcrom jcrom() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(Jcrom.class);
  }

}