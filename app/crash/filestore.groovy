import static play.Play.application

import java.util.SortedSet

import javax.jcr.nodetype.NodeType
import javax.jcr.Session

import com.google.common.collect.ImmutableSortedSet

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Group

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import models.GroupManager
import service.filestore.FileStore
import service.filestore.FileStore.Folder
import service.filestore.roles.Admin
import service.GuiceInjectionPlugin
import service.JcrSessionFactory

@Usage("Filestore operations")
class filestore {

    @Usage("make directories")
    @Command
    void mkdir(
        @Required(true)
        @Argument List<String> directories,
        @Usage("Make parent directories as needed")
        @Option(names=["p","parents"])
        Boolean parents) {
        def filestore = fileStore()
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                def mkdir
                mkdir = { file, parent ->
                    if(file == null) {
                        return filestore.getManager(session).getRoot();
                    } else {
                        Folder parentFolder = mkdir(file.getParentFile(), true);
                        if(parentFolder == null) {
                            return null;
                        }
                        Folder folder = parentFolder.getFolder(file.getName());
                        if(folder == null) {
                            if(parent && (parents == null)) {
                                out.println(String.format(
                                    "Parent folder %s does not exist, try option --parents",
                                    file.getName()));
                                return null;
                            } else {
                                return parentFolder.createFolder(file.getName());
                            }
                        } else {
                            if(!parent) {
                                out.println(String.format(
                                    "Folder %s already exists", file.getName()));
                            }
                            return folder;
                        }
                    }
                }
                directories.each() {
                    mkdir(new File(StringUtils.strip(it)), false);
                }
            }
        })
    }

    @Usage("remove files or directories")
    @Command
    void rm(
        @Required(true)
        @Argument List<String> files,
        @Usage("remove directories and their contents recursively")
        @Option(names=["r", "recursive"])
        Boolean recursive) {
        def filestore = fileStore()
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                def rm;
                rm = { path ->
                    if(path == null) {
                        return null;
                    }
                    def f = filestore.getManager(session).getFileOrFolder(path);
                    if(f == null) {
                        out.println(String.format("no such file or directory '%s'", path));
                    } else if(f.isFolder() && recursive == null) {
                        out.println(String.format(
                            "cannot remove '%s': Is a directory, try using --recursive", path));
                    } else {
                        f.delete();
                    }
                }
                files.each() {
                    rm(it);
                }
            }
        })
    }

  @Usage("print tree of filestore structure")
  @Command
  void tree(
      @Usage("only show tree below folder") @Argument String path,
      @Usage("show permissions") @Option(names=["p", "perms"]) Boolean showPerms
      ) {
    def filestore = fileStore()
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          def sb = new StringBuilder()
          def m = filestore.getManager(session)
          def tree
          tree = { folder ->
            def format = { nw, isFolder ->
              ("|   " * nw.getDepth()).replaceAll(/\|   $/,
                  isFolder ? "|-+ " : "|-- ") +
              // Make root easier to spot/understand
              (nw.getPath() == "/" ? "/ (root)" : nw.getName()) + 
              (showPerms ? " "+nw.getGroupPermissions() : "")
            }
            out.println(format(folder, true))
            folder.getFolders().each {
              tree(it)
            }
            folder.getFiles().each {
              out.println(format(it, false))
            }
          }
          def f = m.getFolder(path == null ? "/" : path);
          if(f!=null) {
              tree(f);
          } else {
              out.println(String.format("no such folder %s", path));
          }
        }
      })
  }

  @Usage("make group filestore administrators")
  @Command
  String listadmin(
      @Usage("group name")
      @Argument
      String name) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          try {
            def root = Admin.getInstance(session).getGroup()
            def tree
            tree = { authorizable, depth ->
              def isGroup = authorizable instanceof Group
              // If group, just use ID
              def n = authorizable.getID()
              try {
                // Get user email if user
                def node = session.getNodeByIdentifier(authorizable.getID())
                n = node.getProperty("email").getValue().getString()
              } catch (Exception e) {}
              out.println(
                ("|   " * depth).replaceAll(/\|   $/,
                    isGroup ? "|-+ " : "|-- ") + n
              )
              if (isGroup) {
                ((Group)authorizable).getDeclaredMembers().each {
                  tree(it, depth + 1)
                }
              }
            }
            tree(root, 0)
          } catch (RuntimeException e) {
            return e.getMessage()
          }
          "\n"
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
  
  private FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(FileStore.class);
  }

}