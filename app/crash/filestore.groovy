import static play.Play.application

import java.util.SortedSet

import javax.jcr.nodetype.NodeType
import javax.jcr.Session

import com.google.common.collect.ImmutableSortedSet

import org.apache.jackrabbit.api.security.user.Group
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import models.GroupManager
import service.filestore.FileStore
import service.filestore.roles.Admin
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import securesocial.core.UserId

@Usage("Filestore operations")
class filestore {

  @Usage("print tree of filestore structure")
  @Command
  void tree() {
    def filestore = new FileStore(sessionFactory())
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          def sb = new StringBuilder()
          def m = filestore.getManager(session)
          def tree
          tree = { folder ->
            def format = { nw, isFolder ->
              ("|   " * nw.getDepth()).replaceAll(/\|   $/,
                  isFolder ? "|-+ " : "|-- ") +
              nw.getName()
            }
            out.println(format(folder, true))
            folder.getFolders().each {
              tree(it)
            }
            folder.getFiles().each {
              out.println(format(it, false))
            }
          }
          tree(m.getRoot())
        }
      })

  @Usage("remove all files and folders")
  @Command
  String destroy() {
    def surePhrase = "Yes, I am."
    def sure = context.readLine(
      "Are you sure? Type \""+surePhrase+"\" to continue.\n",
      true)
    if (!sure.equals(surePhrase)) {
      return
    }
    def filestore = new FileStore(sessionFactory())
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          def root = filestore.getManager(session).getRoot()
          root.getFolders().each { it.delete() }
          root.getFiles().each { it.delete() }
          return "All files and folders have been removed."
        }
      })
  }

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