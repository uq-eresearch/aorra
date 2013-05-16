package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
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
import service.filestore.FileStore.Folder;
import service.filestore.FileStoreImpl;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.google.inject.Inject;

public final class FileStoreController extends SessionAwareController {

  private final FileStore fileStoreImpl;

  @Inject
  public FileStoreController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStoreImpl) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStoreImpl = fileStoreImpl;
  }

  @SubjectPresent
  public Result mkdir(final String encodedPath) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        FileStore.Folder baseFolder = fm.getRoot();
        for (String encodedPart : encodedPath.split("/")) {
          // Skip empty string directories as part oddities
          if (encodedPart.equals("")) continue;
          String part = decodePath(encodedPart);
          final FileStore.FileOrFolder fof = baseFolder.getFileOrFolder(part);
          final FileStore.Folder nextFolder;
          if (fof == null) {
            nextFolder = baseFolder.createFolder(part);
          } else if (fof instanceof FileStore.Folder) {
            nextFolder = (FileStore.Folder) fof;
          } else {
            throw new ItemExistsException(
                "Path item exists which is not a folder.");
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
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
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
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+filePath);
        if (fof instanceof FileStoreImpl.File) {
          FileStore.File file = (FileStoreImpl.File) fof;
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
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof;
        try {
          fof = fm.getFileOrFolder("/"+URLDecoder.decode(folderPath, "UTF-8"));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        if (fof == null) {
          return badRequest("A valid folder must be specified.")
              .as("text/plain");
        }
        final FileStore.Folder folder;
        if (fof instanceof FileStore.Folder) {
          folder = (Folder) fof;
        } else {
          return badRequest("Specified destination is not a folder.")
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
      final FileStore.FileOrFolder fof =
          folder.getFileOrFolder(filePart.getFilename());
      final FileStore.File f;
      if (fof == null) {
        f = folder.createFile(filePart.getFilename(),
            filePart.getContentType(),
            new FileInputStream(filePart.getFile()));
      } else if (fof instanceof FileStore.File) {
        f = (FileStore.File) fof;
        f.update(filePart.getContentType(),
          new FileInputStream(filePart.getFile()));
      } else {
        throw new ItemExistsException("Item exists and is not a file.");
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