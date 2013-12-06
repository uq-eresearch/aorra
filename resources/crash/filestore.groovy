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
import helpers.FileStoreHelper

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
            def fsh = new FileStoreHelper(session, out);
            directories.each() {
              fsh.mkdir(StringUtils.strip(it), parents == true);
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
          def fsh = new FileStoreHelper(session, out);
          files.each() {
            fsh.rm(StringUtils.strip(it), Boolean.valueOf(recursive));
          }
        }
      })
    }

    @Usage("move (rename) files or directories")
    @Command
    void mv(
      @Required(true)
      @Argument String src,
      @Required(true)
      @Argument String dest) {
      def filestore = fileStore()
      sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
          def fsh = new FileStoreHelper(session, out);
          fsh.mv(src, dest);
        }
      })
    }

  @Usage("print tree of filestore structure")
  @Command
  void tree(
      @Usage("only show tree below folder") @Argument String path,
      @Usage("show permissions") @Option(names=["p", "perms"]) Boolean showPerms,
      @Usage("show ids") @Option(names=["i", "ids"]) Boolean showIds
      ) {
    def filestore = fileStore()
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
            def fsh = new FileStoreHelper(session, out);
            fsh.tree((String)path, showPerms, showIds);
        }
      })
  }

  @Usage("list group filestore administrators")
  @Command
  String listadmin(
      @Usage("group name")
      @Argument
      String name) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
            def fsh = new FileStoreHelper(session, out);
            fsh.listadmin(name);
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
          name + " granted admin access.\n"
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
          name + " stripped of admin access.\n"
        }
      })
  }

  @Usage("import the contents of a zip file into the filestore")
  @Command
  String importArchive(
    @Required(true)
    @Usage("path to zip file")
    @Argument
    String zipfile,
    @Required(false)
    @Usage("import into folder path (defaults to '/')")
    @Argument
    String path
    ) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
            def fsh = new FileStoreHelper(session, out);
            fsh.importArchive(zipfile, path);
        }
      })
  }

  @Usage("show file versions")
  @Command
  String versions(
    @Required(true)
    @Usage("path to file")
    @Argument
    String path) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
            def manager = fileStore().getManager(session);
            def fof = manager.getFileOrFolder(path);
            SortedMap<String,FileStore.File> versions = fof.getVersions();
            for(Map.Entry<String, FileStore.File> me : versions) {
                out.println(me.getKey()+" - "+me.getValue().getPath());
            }
        }
      })
  }

  @Usage("show file/folder information")
  @Command
  String info(
    @Required(true)
    @Usage("path or id of file/folder")
    @Argument
    String pathOrId) {
    sessionFactory().inSession(new Function<Session, String>() {
        public String apply(Session session) {
            def fsh = new FileStoreHelper(session, out);
            fsh.printInfo(pathOrId);
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