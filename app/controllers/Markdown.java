package controllers;

import javax.jcr.Session;

import org.apache.tika.io.IOUtils;
import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;

import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Markdown extends SessionAwareController {

    private final FileStore fileStore;

    @Inject
    public Markdown(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
        final CacheableUserProvider sessionHandler, final FileStore fileStore) {
      super(sessionFactory, jcrom, sessionHandler);
      this.fileStore = fileStore;
    }

    @SubjectPresent
    public Result toHtml(final String fileId) {
        try {
            return ok(new markdown.Markdown().toHtml(content(fileId))).as("text/html");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubjectPresent
    public Result toHtmlZip(final String fileId) {
        try {
            return ok(new markdown.Markdown().toHtmlZip(filename(fileId), content(fileId),
                    request().cookie("PLAY_SESSION").value())).as("application/zip");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubjectPresent
    public Result toPdf(final String fileId, String converter, String copts) {
        try {
            return ok(new markdown.Markdown().toPdf(filename(fileId), content(fileId),
                    request().cookie("PLAY_SESSION").value(), converter, copts)).as("application/pdf");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String content(final String fileId) {
        return inUserSession(new F.Function<Session, String>() {
            @Override
            public final String apply(Session session) throws Exception {
                final FileStore.Manager fm = fileStore.getManager(session);
                FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
                if (fof instanceof FileStore.File) {
                    FileStore.File file = (FileStore.File) fof;
                    return IOUtils.toString(file.getData());
                } else {
                    throw new RuntimeException("folder");
                }
            }
        });
    }

    private String filename(final String fileId) {
        return inUserSession(new F.Function<Session, String>() {
            @Override
            public final String apply(Session session) throws Exception {
                final FileStore.Manager fm = fileStore.getManager(session);
                FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
                if (fof instanceof FileStore.File) {
                    FileStore.File file = (FileStore.File) fof;
                    return file.getName();
                } else {
                    throw new RuntimeException("folder");
                }
            }
        });
    }

}
