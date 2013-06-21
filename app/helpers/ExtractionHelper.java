package helpers;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;

public class ExtractionHelper {

    private Session session;

    private String path;

    private String version;

    public ExtractionHelper(Session session, String path) {
        this.session = session;
        this.path = path;
    }

    public ExtractionHelper(Session session, String path, String version) {
        this(session, path);
        this.version = version;
    }

    public String getPlainText() throws RepositoryException {
        BodyContentHandler handler = new BodyContentHandler();
        parse(handler, new Metadata());
        return handler.toString();
    }

    public Metadata getMetadata() throws RepositoryException {
        Metadata metadata = new Metadata();
        parse(new BodyContentHandler(), metadata);
        return metadata;
    }

    private void parse(ContentHandler handler, Metadata metadata) throws RepositoryException {
        FileStore.File file;
        FileStore.FileOrFolder fof = fileStore().getManager(session).getFileOrFolder(path);
        if(fof instanceof FileStore.File) {
            file = (FileStore.File)fof;
        } else {
            throw new RuntimeException("not a file: "+path);
        }
        file = getVersion(version, file);
        try {
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext ctx = new ParseContext();
            parser.parse(file.getData(), handler, metadata, ctx);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FileStore.File getVersion(String version, FileStore.File file) throws RepositoryException {
        for(Map.Entry<String, FileStore.File> me : file.getVersions().entrySet()) {
            if(StringUtils.equals(version, me.getKey())) {
                return me.getValue();
            }
        }
        return file;
    }

    protected FileStore fileStore() {
        return GuiceInjectionPlugin.getInjector(Play.application())
                                   .getInstance(FileStore.class);
    }

}
