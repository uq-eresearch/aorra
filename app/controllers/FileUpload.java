package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.libs.F.Function;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import service.JcrSessionFactory;
import service.filestore.FileStore;

import com.google.inject.Inject;

public final class FileUpload extends Controller {

  private final FileStore fileStore;
  private final JcrSessionFactory sessionFactory;

  @Inject
  public FileUpload(final JcrSessionFactory sessionFactory,
      final FileStore fileStore) {
    this.fileStore = fileStore;
    this.sessionFactory = sessionFactory;
  }

  public Result postUpload(final String folderPath) {
    return sessionFactory.inSession(new Function<Session, Result>() {
      @Override
      public final Result apply(Session session) {
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
            {
              updateFileContents(folder, filePart);
              final String fileName = filePart.getFilename();
              final File file = filePart.getFile();
              //Logger.info(String.format(
              //  "file %s content type %s uploaded to %s by %s",
              //  fileName, filePart.getContentType(), file.getAbsolutePath(),
              //  user.id().id()));
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

  public Result getUpload() {
    return ok(views.html.FileUpload.upload.render());
  }

  private void updateFileContents(FileStore.Folder folder,
      MultipartFormData.FilePart filePart) {
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
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

}
