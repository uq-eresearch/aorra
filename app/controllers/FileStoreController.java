package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jcrom.Jcrom;

import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.google.inject.Inject;

public final class FileStoreController extends SessionAwareController {

  private final FileStore fileStore;

  @Inject
  public FileStoreController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStore) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStore = fileStore;
  }

  @SubjectPresent
  public Result mkdir(final String encodedPath) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStore.getManager(session);
        FileStore.Folder baseFolder = fm.getRoot();
        for (String encodedPart : encodedPath.split("/")) {
          // Skip empty string directories as part oddities
          if (encodedPart.equals("")) continue;
          String part = decodePath(encodedPart);
          Logger.debug(part);
          FileStore.Folder nextFolder = baseFolder.getFolder(part);
          if (nextFolder == null) {
            nextFolder = baseFolder.createFolder(part);
          }
          // Move up
          baseFolder = nextFolder;
        }
        return created();
      }
    });
  }

  @SubjectPresent
  public Result delete(final String encodedPath) {
    final String path = decodePath(encodedPath);
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStore.getManager(session);
        FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+path);
        fof.delete();
        return noContent();
      }
    });
  }

  @SubjectPresent
  public Result download(final String encodedFilePath) {
    final String filePath = decodePath(encodedFilePath);
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStore.getManager(session);
        FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+filePath);
        if (fof instanceof FileStore.File) {
          FileStore.File file = (FileStore.File) fof;
          return ok(file.getData()).as(file.getMimeType());
        } else {
          return notFound();
        }
      }
    });
  }

  @SubjectPresent
  public Result postUpload(final String folderPath) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStore.getManager(session);
        final FileStore.Folder folder;
        try {
          folder = fm.getFolder("/"+folderPath);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        if (folder == null) {
          return badRequest("A valid folder must be specified.")
              .as("text/plain");
        }
        final MultipartFormData body = request().body().asMultipartFormData();
        if (body == null) {
          return badRequest("POST must contain multipart form data.")
              .as("text/plain");
        }
        // Assemble the JSON response
        final ObjectNode json = Json.newObject();
        {
          final ArrayNode aNode = json.putArray("files");
          for (MultipartFormData.FilePart filePart : body.getFiles()) {
            final ObjectNode jsonFileData = Json.newObject();
            try {
              FileStore.File f = updateFileContents(folder, filePart);
              session.save();
              final String fileName = filePart.getFilename();
              final File file = filePart.getFile();
              Logger.info(String.format(
                "file %s content type %s uploaded to %s by %s",
                fileName, filePart.getContentType(),
                f.getPath(),
                getUser()));
              jsonFileData.put("name", fileName);
              jsonFileData.put("size", file.length());
            } catch (AccessDeniedException ade) {
              return forbidden(String.format(
                  "Insufficient permissions to upload files to %s",
                  folder.getPath()));
            }
            aNode.add(jsonFileData);
          }
        }
        // even though we return json set the content type to text/html
        // to prevent IE/Opera from opening a download dialog as described here:
        // https://github.com/blueimp/jQuery-File-Upload/wiki/Setup
        return ok(json).as("text/html");
      }
    });
  }

  private FileStore.File updateFileContents(FileStore.Folder folder,
      MultipartFormData.FilePart filePart) throws RepositoryException {
    try {
      FileStore.File f = folder.getFile(filePart.getFilename());
      if (f == null) {
        f = folder.createFile(filePart.getFilename(),
            filePart.getContentType(),
            new FileInputStream(filePart.getFile()));
      } else {
        f.update(filePart.getContentType(),
          new FileInputStream(filePart.getFile()));
      }
      return f;
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decodePath(String path) {
    try {
      return URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never happen
      throw new RuntimeException(e);
    }
  }

}