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
import service.filestore.FileStore.FileOrFolder
import service.filestore.roles.Admin
import service.GuiceInjectionPlugin
import service.JcrSessionFactory
import helpers.FileStoreHelper
import helpers.ExtractionHelper
import org.apache.tika.metadata.Metadata;

@Usage("document extraction opertations")
class extract {

    @Usage("show plain text extraction from document")
    @Command
    void text(
        @Usage("path to a document")
        @Required(true)
        @Argument String path,
        @Usage("version") @Option(names=["v", "version"]) String version) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            def extractor = new ExtractionHelper(session, path, version);
            out.println(extractor.getPlainText());
          }
        })
    }

    @Usage("show metadata of document")
    @Command
    void metadata(
        @Usage("path to a document")
        @Required(true)
        @Argument String path,
        @Usage("version") @Option(names=["v", "version"]) String version) {
        sessionFactory().inSession(new Function<Session, String>() {
          public String apply(Session session) {
            def extractor = new ExtractionHelper(session, path, version);
            Metadata metadata = extractor.getMetadata();
            for(String name : metadata.names()) {
                out.println(name+": "+StringUtils.join(metadata.getValues(name), ", "));
            }
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