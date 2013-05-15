import static play.Play.application

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.Repository;
import javax.jcr.Value;

import org.apache.jackrabbit.core.RepositoryCopier;
import org.apache.jackrabbit.core.RepositoryImpl;

import org.apache.commons.lang.StringUtils;

import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.cli.Option
import play.Play
import play.libs.F.Function
import service.GuiceInjectionPlugin
import service.JcrSessionFactory


@Usage("Repository export")
class repo {

    @Usage("export repository")
    @Command
    void export(
        @Usage("absolute path of a node")
        @Argument String path,
        @Usage("recursive export all child nodes")
        @Option(names=["r","recursive"])
        Boolean recursive,
        @Usage("export to file (if omitted export is written to screen)")
        @Option(names=["f","file"])
        String file,
        @Usage("skip binary data otherwise binary data is added in base64 encoding")
        @Option(names=["s","skip-binary"])
        Boolean skipBinary,
        @Usage("export system view (instead of document view)")
        @Option(names=["system"])
        Boolean systemView
        ) {
        if(StringUtils.isBlank(path)) {
            path = "/";
        }
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                def stream;
                if(file != null) {
                    stream = new FileOutputStream(new File(file));
                } else {
                    stream = new OutputStream() {
                        public void write(int b) throws IOException {
                            repo.this.out.write(b);
                        }
                    }
                }
                if(systemView!=null) {
                    session.exportSystemView(path, stream, skipBinary != null, recursive == null);
                } else {
                    session.exportDocumentView(path, stream, skipBinary != null, recursive == null);
                }
                stream.close();
            }
        })
    }
    
    @Usage("export repository")
    @Command
    void importXml(
        @Usage("absolute path of a node")
        @Argument String path, 
        @Usage("import from file")
        @Option(names=["f","file"])
        String file) {
        if(StringUtils.isBlank(path)) {
            path = "/";
        }
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                def stream = new FileInputStream(new File(file));
                Workspace  workspace = session.getWorkspace();
                workspace.importXML(path, stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            }
        });
    }


    @Usage("list repository descriptors")
    @Command
    void descriptors() {
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                Repository rep = session.getRepository();
                String[] keys = rep.getDescriptorKeys();
                for(String str : keys) {
                    Value val = rep.getDescriptorValue(str);
                    String valStr;
                    if(val != null) {
                        valStr = val.getString();
                    } else {
                        valStr = "";
                    }
                    out.println(str+" - "+valStr);
                }
            }
        });
    }

    @Usage("copy repository to folder")
    @Command
    void copy(
        @Usage("absolute folder path")
        @Required
        @Argument String path) {
        sessionFactory().inSession(new Function<Session, String>() {
            public String apply(Session session) {
                out.println(path);
                File folder = new File(path);
                RepositoryImpl rep = (RepositoryImpl)session.getRepository();
                RepositoryCopier.copy(rep, folder);
            }
        });
    }

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }

}
