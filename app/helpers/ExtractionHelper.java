package helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import scala.Tuple2;
import play.Logger;
import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;

public class ExtractionHelper {

  private final FileStore.File file;

  public ExtractionHelper(FileStore.File file) {
    this.file = file;
  }

  public ExtractionHelper(Session session, String path)
      throws RepositoryException {
    this.file = getFile(session, path);
  }

  public ExtractionHelper(Session session, String path, String version)
      throws RepositoryException {
    this.file = getVersion(version, getFile(session, path));
  }

  protected FileStore.File getFile(Session session, String path)
      throws RepositoryException {
    final FileStore.FileOrFolder fof = fileStore().getManager(session)
        .getFileOrFolder(path);
    if (!(fof instanceof FileStore.File)) {
      throw new PathNotFoundException("not a file: " + path);
    }
    return (FileStore.File) fof;
  }

  public String getPlainText() {
    final StringWriter sw = new StringWriter();
    parse(new BodyContentHandler(sw) {
      @Override
      public void startElement(String uri, String localName, String name,
          Attributes atts) throws SAXException {
        if ("img".equals(localName)) {
          final String alt = atts.getValue("alt").isEmpty()
              ? "image"
              : atts.getValue("alt");
          sw.write("\n!["+alt+"]("+atts.getValue("src")+")\n");
        }
        super.startElement(uri, localName, name, atts);
      }
    });
    return sw.toString();
  }

  public Metadata getMetadata() {
    return parse(new DefaultHandler())._2();
  }

  private Tuple2<ContentHandler,Metadata> parse(ContentHandler handler) {
    final Metadata metadata = new Metadata();
    final AutoDetectParser parser = new AutoDetectParser();
    try {
      parser.parse(file.getData(), handler, metadata, new ParseContext());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (TikaException e) {
      throw new RuntimeException(e);
    }
    return new Tuple2<ContentHandler,Metadata>(handler, metadata);
  }

  private FileStore.File getVersion(String version, FileStore.File file)
      throws RepositoryException {
    final Map<String, FileStore.File> versions = file.getVersions();
    if (versions.containsKey(version)) {
      return versions.get(version);
    } else {
      return file;
    }
  }

  protected FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(Play.application()).getInstance(
        FileStore.class);
  }


}
