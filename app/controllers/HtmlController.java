package controllers;

import html.FileCleanup;
import html.HtmlToPdf;
import html.HtmlZip;
import html.XToHtml;

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
public class HtmlController extends SessionAwareController {

    private final FileStore fileStore;

    @Inject
    public HtmlController(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
        final CacheableUserProvider sessionHandler, final FileStore fileStore) {
      super(sessionFactory, jcrom, sessionHandler);
      this.fileStore = fileStore;
    }

    @SubjectPresent
    public Result toHtml(final String fileId) {
        try {
            String h = html(fileId);
            return h!=null?ok(h).as("text/html"):notFound("can not convert this file to html");
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubjectPresent
    public Result toHtmlZip(final String fileId) {
        FileCleanup c = null;
        try {
            String h = html(fileId);
            if(h!=null) {
                 c = new HtmlZip().toHtmlZip(filename(fileId), h,
                        request().cookie("PLAY_SESSION").value());
                ctx().response().setHeader("Content-Disposition",
                        "attachment; filename="+filename(fileId)+".zip");
                return ok(c.result()).as("application/zip");
            } else {
                return notFound("can not convert this file to html");
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c!= null) {
                c.cleanup();
            }
        }
    }

    @SubjectPresent
    public Result toPdf(final String fileId, String converter, String copts) {
        FileCleanup c = null;
        try {
            String h = html(fileId);
            if(h!=null) {
                c = new HtmlToPdf().toPdf(filename(fileId), html(fileId),
                        request().cookie("PLAY_SESSION").value(), converter, copts);
                ctx().response().setHeader("Content-Disposition",
                        "attachment; filename="+filename(fileId)+".pdf");
                return ok(c.result()).as("application/pdf");
            } else {
                return notFound("can not convert this file to pdf");
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(c!= null) {
                c.cleanup();
            }
        }
    }

    private String html(final String fileId) {
        return new XToHtml().toHtml(content(fileId), mimetype(fileId));
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

    private String mimetype(final String fileId) {
        return inUserSession(new F.Function<Session, String>() {
            @Override
            public final String apply(Session session) throws Exception {
                final FileStore.Manager fm = fileStore.getManager(session);
                FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
                if (fof instanceof FileStore.File) {
                    FileStore.File file = (FileStore.File) fof;
                    return file.getMimeType();
                } else {
                    throw new RuntimeException("folder");
                }
            }
        });
    }

}
