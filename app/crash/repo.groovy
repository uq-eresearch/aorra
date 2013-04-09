import static play.Play.application

import javax.jcr.Session

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

  private JcrSessionFactory sessionFactory() {
    return GuiceInjectionPlugin.getInjector(application())
                               .getInstance(JcrSessionFactory.class);
  }

}
