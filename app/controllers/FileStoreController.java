package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.MultipartFormData;
import service.JcrSessionFactory;
import service.filestore.FileStore;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.google.inject.Inject;

public final class FileStoreController extends Controller {

  private final service.filestore.FileStore fileStore;
  private final JcrSessionFactory sessionFactory;

  @Inject
  public FileStoreController(final JcrSessionFactory sessionFactory,
      final service.filestore.FileStore fileStore) {
    this.fileStore = fileStore;
    this.sessionFactory = sessionFactory;
  }

  @Security.Authenticated(Secured.class)
  public Result upload() {
    return ok(views.html.FileStoreController.upload.render());
  }

  @Security.Authenticated(Secured.class)
  public Result postUpload(final String folderPath) {
    return sessionFactory.inSession(new Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStore.getManager(session);
        final AuthUser user = PlayAuthenticate.getUser(ctx());
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
            {
              FileStore.File f = updateFileContents(folder, filePart);
              final String fileName = filePart.getFilename();
              final File file = filePart.getFile();
              Logger.info(String.format(
                "file %s content type %s uploaded to %s by %s",
                fileName, filePart.getContentType(),
                f.getPath(),
                user.getId()));
              jsonFileData.put("name", fileName);
              jsonFileData.put("size", file.length());
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

}